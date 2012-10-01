package siris.core.entity.description

import actors.Actor
import concurrent.SyncVar
import collection.mutable
import siris.core.entity.Entity
import siris.core.entity.component._
import siris.core.entity.typeconversion._
import siris.core.svaractor.{SVarActorImpl, SVarActorLW}
import siris.core.component.Component
import java.lang.Exception


/**
 *  the interface for entity descriptions.
 *
 */
trait EntityDescriptionInterface[+Type <: Entity] {
  def realize( handler : Type with Removability => Any )
  def realize( toNotify : Actor )
  def realize() : Type
}

/**
 * @author dwiebusch
 * date: 24.08.2010
 */

object IEntityDescription{
  private var observers = Set[Component]()

  def registerCreationObserver(observer : Component) { synchronized {
    observers = observers + observer
  } }

  def removeCreationObserver(observer : Component) { synchronized {
    observers = observers - observer
  } }

  def notifyObservers( msg : Any ) {
    observers.foreach( _ ! msg )
  }

  def propagateEntityRemoval( e : Entity ) {
    observers.foreach( _ ! RemoveEntityMessage(Actor.self, e) )
  }
}

/**
 * @author dwiebusch
 * Date: 09.05.11
 * Time: 09:32
 */

class IEntityDescription[Type <: Entity] protected(val typeDef : ConvertibleTrait[Type],
                                                   createType  : Entity with Removability => Type with Removability,
                                                   theHandler  : Option[Type with Removability => Any],
                                                   aspects     : Seq[AspectBase] )
  extends SVarActorLW with EntityDescriptionInterface[Type] with AspectBase
{
  //initially check validity of this description
  initialCheck(aspects.map(_.toEntityAspect))

  /**
   *  creates a new entity described by this description and executes the given handler afterwards
   * @param handler the handler executed after the creation process is finished
   */
  def realize(handler: (Type with Removability) => Any) {
    new IEntityDescription[Type](typeDef, createType, Some(handler), aspects).start ! PreProcess(aspects.toList)
  }

  /**
   *  creates a new entity described by this description and sends an EntityCreatedMessage to the given actor
   *        afterwards
   * @param toNotify the actor to be sent the EntityCreatedMessage
   */
  def realize(toNotify: Actor) {
    realize(toNotify ! EntityCreatedMessage( _ : Entity with Removability))
  }


  /**
   *  creates a new entity described by this description and blocks the current thread until the creation process
   *        is finished. HANDLE WITH CARE!!!
   */
  def realize() = {
    // TODO: Remove SyncVar!!
    val wait = new SyncVar[Type with Removability]
    realize( wait.set _ )
    wait.get
  }

  def getFeatures =
    throw new Exception()

  def getProvidings =
    throw new Exception()

  def toEntityAspect =
    throw new Exception()

  //////
  //! internal case class, used to start the creation process
  private case class PreProcess(aspects : List[AspectBase], done : List[AspectBase] = Nil, es : SValList = new SValList)

  addHandler[PreProcess]{
    msg => preProcess( msg.aspects, msg.done, msg.es )
  }

  private def preProcess( aspects : List[AspectBase], done : List[AspectBase], es : SValList ) {
    aspects match {
      case Nil          => startCreation( done.map(_.toEntityAspect), es )
      case head :: tail => head match {
        case eDesc : IEntityDescription[_] => realizeHelper( eDesc, es, PreProcess(tail, done, _) )
        case _ => preProcess( tail, head :: done, es )
      }
    }
  }

  private def realizeHelper[T <: Entity]( eDesc : IEntityDescription[T], es : SValList, ppMsg : SValList => PreProcess){
    eDesc.realize{ (e : T with Removability) => Actor.self ! ppMsg(es += eDesc.typeDef.apply(e)) }
  }

  // some typedefs
  private type TripleSet = (Actor, EntityAspect, Set[ConvertibleTrait[_]])
  private type Triple = (Actor, EntityAspect, Dependencies)

  //! the handler which notifies creation observers via the EntityDescription object
  private val myHandler = (e : Type with Removability, s : Seq[EntityAspect]) => {
    IEntityDescription.notifyObservers( EntityConfiguration(e, s.map( toTuple _).toMap) ); e
  }

  //! dependencies for each component
  private var storedDeps = Map[Actor, Map[EntityAspect, Set[Dependencies]]]()
  //! the buildorder
  private var sortedDeps    = List[TripleSet]()
  //! set used to keep track of the sent and unanswered GetDependencies requests
  private val openGetDepRequests = mutable.Set[Actor]()
  //! set used to keep track of the sent and unanswered EntityComplete messages
  private val openCreateNotifications = mutable.Set[Actor]()
  //! set to store the initial values
  private val values        = new SValList
  //! the entity to be filled with svars
  private val theEntity     = new Entity with Removability with PluggableMutability
  //! helper for concise code
  private var entityAspects = Seq[EntityAspect]()
  //! user defined overrides (isOwned)
  private var ownerMap      = Map[Symbol, Actor]()

  /**
   *  creates a tuple of componentType and NamedSValList from a given EntityAspect
   * @param a the EntityAspect
   * @return the resulting tuple
   */
  private def toTuple(a : EntityAspect) : (Symbol, NamedSValList) =
    a.componentType.value.toSymbol -> a.createParamSet

  /**
   *  checks the description fort validity. Throws an InvalidEntityDescriptionException if it's not
   */
  private def initialCheck( eAspects : Seq[EntityAspect] ) {
    val provideMap        = collection.mutable.Map[ConvertibleTrait[_], Set[EntityAspect]]()
    val ownerMap          = collection.mutable.Map[ConvertibleTrait[_], Set[EntityAspect]]()
    val ProvideOverrides  = collection.mutable.Map[ConvertibleTrait[_], EntityAspect]()
    val OwnerOverrides    = collection.mutable.Map[ConvertibleTrait[_], EntityAspect]()
    val Features          = eAspects.flatMap(_.getFeatures).toSet
    val Providings        = eAspects.flatMap(_.getProvidings).toSet
    val missingProvidings = Features -- Providings

    //store overrides
    val multipleOverrides = eAspects.foldLeft(""){ (str, aspect) => { aspect.overrides.foldLeft(str){
      (s, overr) => overr match {
        case Provide(c) if (ProvideOverrides.getOrElseUpdate(c.from, aspect) != aspect) =>
          s + "\t" + c.from + " shall be provided by " + ProvideOverrides(c.from).componentType +
            " and " +  aspect.componentType + "\n"
        case Own(c) if (OwnerOverrides.getOrElseUpdate(c, aspect) != aspect) =>
          s + "\t" + c + " shall be owned by " + OwnerOverrides(c).componentType
          " and " + aspect.componentType + "\n"
        case _ => s + ""
      }
    }}}
    //check for invalid assertions
    val invalidAssertions = for (asp <- eAspects) yield {
      //check if providings is a subset of features
      val invalidProvidings = asp.getProvidings.filterNot(asp.getFeatures.contains)
      //check for invalid overrides
      val invalidProvides   = collection.mutable.Set[ConvertibleTrait[_]]()
      val invalidOwnerships = collection.mutable.Set[ConvertibleTrait[_]]()
      asp.overrides.foreach{
        _ match {
          case Provide(c) =>
            if (ProvideOverrides.contains(c.from))
              provideMap.update(c.from, Set(ProvideOverrides(c.from)))
            else
              provideMap.update(c.from, provideMap.getOrElseUpdate(c.from, Set()) + asp)
            if (!asp.getProvidings.contains(c.from))
              invalidProvides += c.from
          case Own(c) =>
            if (OwnerOverrides.contains(c))
              ownerMap.update(c, Set(OwnerOverrides(c)))
            else
              ownerMap.update(c, ownerMap.getOrElseUpdate(c, Set()) + asp)
            if (!Features.contains(c))
              invalidOwnerships += c
          case _ =>
        }
      }
      (asp, invalidProvidings, invalidProvides, invalidOwnerships)
    }
    //create exceptionText
    var exceptionText = if (multipleOverrides.isEmpty) "" else "Multiple overridings:\n" + multipleOverrides
    for (mp <- missingProvidings) exceptionText += "\t" + mp + " is never provided\n"
    for ((c, owners) <- ownerMap)
      if (owners.size > 1)
        exceptionText += "\t" +c + " shall be owned by multiple aspects: " + owners + "\n"
    for ((c, providers) <- provideMap)
      if (providers.size > 1)
        exceptionText += "\t" +c + " shall be provided by multiple aspects: " + providers + "\n"
    for (ia <- invalidAssertions) {
      ia._2.foreach( c => exceptionText += "\t" + c + " in " + ia._1 + " shall be provided but is not included in the feature list\n" )
      ia._3.foreach( c => exceptionText += "\t" + c + " in " + ia._1 + " shall be provided but is not included in the providings list\n" )
      ia._4.foreach( c => exceptionText += "\t" + c + " in " + ia._1 + " shall be owned but is not provided anywhere\n" )
    }
    if (exceptionText.nonEmpty)
      throw InvalidEntityDescriptionException( this.asInstanceOf[IEntityDescription[Entity]], exceptionText )
  }

  /**
   *  Creates a map (T, Actor) by applying the given partial function to all provides and requires of each aspect.
   *        The Functions result (if it can be applied) is added as key to the map (value = the associated component).
   * @param func the partial function to be appplied
   * @return a map with func's results as keys and associated components as values
   */
  private def extractPar[T]( func : PartialFunction[ProvideAndRequire, Option[T]]) = {
    val fallback = { case _ => None } : PartialFunction[ProvideAndRequire, Option[T]]
    entityAspects.foldLeft(Map[T, Actor]()){
      (map, aspect) =>
        val theComponent = Component(aspect.componentType).head
        aspect.overrides.foldLeft(map){
          (m, elem) => func.orElse(fallback).apply(elem).collect{ case x => m.updated(x, theComponent) }.getOrElse(m)
        }
    }
  }

  private def createProviding[T]( sval : SVal[T] ) : Provide[T, _] =
    sval.typedSemantics.isProvided.withInitialValue(sval.value)

  //start the entity creation process by sending get dependencies messages
  private def startCreation( aspects : Seq[EntityAspect], subelements : SValList ) {
    entityAspects = aspects
    ownerMap      = extractPar{ case Own(c) => Some(c.sVarIdentifier) }
    subelements.foreach( x => theEntity.injectSVar(createProviding(x) ) )
    entityAspects.foreach{ aspect =>
      Component(aspect.componentType).headOption.foreach{ c =>
        c ! GetDependenciesMsg( Actor.self, aspect )
        openGetDepRequests += c
      }
    }
  }

  //process dependencies
  addHandler[GetDependenciesAns]{ msg : GetDependenciesAns =>
    storedDeps = storedDeps.updated(msg.sender, storedDeps.getOrElse(msg.sender, Map()) + (msg.asp -> mergeProvidings(msg.asp, msg.deps)))
    openGetDepRequests.remove(msg.sender)
    if (openGetDepRequests.isEmpty){
      storedDeps = filterProvidings(storedDeps, extractPar{ case Provide(c) => Some(c.getSVarName) } )
      sortedDeps = sortDependencies(storedDeps)
      checkForDoubles(sortedDeps)
      setOwners(sortedDeps)
      requestInitialValues(sortedDeps)
    }
  }

  /**
   *  adds the providings from the given entity aspect to the given set of dependencies, if they are not contained
   *        already.
   * @param a the aspect to be merged
   * @param ds the given set of dependencies
   * @return the merged set
   */
  private def mergeProvidings( a : EntityAspect, ds : Set[Dependencies] ) : Set[Dependencies] = {
    val toAdd = a.getProvidings.filter(p => ds.forall(!_.providings.objects.contains(p)))
    if (toAdd.nonEmpty) ds + Dependencies(Providing(toAdd.toSeq : _* )) else ds
  }

  /**
   *  filters the dependencies provided by each component and adds/removes user defined providings (isProvided)
   * @param m the map to be filtered
   */
  private def filterProvidings(m : Map[Actor, Map[EntityAspect, Set[Dependencies]]], providings : Map[Symbol, Actor]) =
    m.foldLeft(m.empty){ (map, tuple) =>
      map.updated(tuple._1,  tuple._2.map{ iTuple => {
        val correctedDeps = handleDeps(tuple._1, iTuple._2, providings)
        if (correctedDeps.isEmpty) iTuple else (iTuple._1 -> correctedDeps)
      } } )
    }

  /**
   *  applies the handle dep function to each dependency in the given set, resulting in a set of dependencies
   *        from which all providings that were overridden by the user are removed
   * @return the cleaned set
   */
  private def handleDeps(actor : Actor, dep : Set[Dependencies], providings : Map[Symbol, Actor]) : Set[Dependencies] =
    dep.foldLeft(dep.empty){
      (set, elem) => handleDep(actor, elem, providings).collect{ case f => set + f }.getOrElse(set)
    }

  /**
   *  checks for each providing in the given dependencies if it shall be provided by the given actor. If it shall
   *        be provided by another actor, it is removed from the dependency
   * @return an option containing the cleaned dependencies, if it still contains providings, None otherwise
   */
  private def handleDep( actor : Actor, dep : Dependencies, providings : Map[Symbol, Actor] ) : Option[Dependencies] = {
    val filtered = dep.providings.objects.filter( p => providings.getOrElse(p.sVarIdentifier, actor) == actor )
    if (filtered.isEmpty) None else Some(Dependencies(Providing(filtered : _*), dep.requirings))
  }

  /**
   *  creates the buildorder from the given dependency map
   * @param deps the input dependency map
   * @return a ordered list of type TripleSet which is sorted according to the given dependencies
   */
  private def sortDependencies(deps : Map[Actor, Map[EntityAspect, Set[Dependencies]]]) : List[TripleSet] = {
    val tmp = deps.flatMap( elem => elem._2.flatMap( x => x._2.map( (elem._1, x._1, _) ).toList ) ).toList
    val (pre, toSort) = split[Triple](tmp,  _._3.requirings.objects.isEmpty)
    merge( pre ::: sort(toSort, pre.flatMap(_._3.providings.objects).toSet) )
  }

  /**
   *  splits the given list into a tuple of a list containing the elements that match the given filter
   *        and a list containing the elements that don't
   * @param toFilter the list to split
   * @param filter the filter according to which the input list is split
   * @return a tuple of the two resulting lists
   */
  private def split[T]( toFilter : List[T], filter : (T) => Boolean) : (List[T], List[T]) =
    Tuple2(toFilter.filter(filter), toFilter.filterNot(filter))

  /**
   *  sorts the given list of triples and creates an ordered list of triples that is sorted by dependencies
   * @param toSort the list of triples to be sorted
   * @param known the set of convertible traits which is already provided
   * @return the ordered list
   */
  private def sort(toSort : List[Triple], known : Set[ConvertibleTrait[_]]) : List[Triple] = {
    if (toSort.isEmpty)
      return toSort
    val (found, remaining) = split(toSort, (_ : Triple)._3.requirings.objects.forall( known.contains ) )
    if (found.isEmpty) throw ResolveRequirementsException(aspects.toString())
    found ::: sort(remaining, found.foldLeft(known){ (set, elem) => set ++ elem._3.providings.objects } )
  }

  /**
   *  merges elements of the given buildorder, when possible
   * @param list the buildorder
   * @param currentSet the set of providings which has been collected previously (for internal usage)
   * @return the merged list
   */
  private def merge(list : List[Triple], currentSet : Set[ConvertibleTrait[_]] = Set()) : List[TripleSet] = list match{
    case Nil => Nil
    case (actor, asp, dep) :: Nil  => (actor, asp, currentSet ++ dep.providings.objects) :: Nil
    case head :: tail if cont(head, tail.head) => merge(tail, currentSet ++ head._3.providings.objects)
    case (actor, asp, dep) :: tail => (actor, asp, currentSet ++ dep.providings.objects ) :: merge(tail)
  }

  /**
   *  checks if the given actor is responsible for the given next triple and if the requirings of the next triple
   *        is a subset of the requirings of the given dependencies
   * @param actor the actor which is checked for responsibility
   * @param the dependencies which requirings have to be a subset of the next triples requirings
   * @param next the next triple
   * @return true if both conditions hold
   */
  private def cont(current : Triple, next : Triple) : Boolean =
    current._1 == next._1 && current._2 == next._2 &&
      next._3.requirings.objects.forall(current._3.requirings.objects.contains)

  /**
   *  thows a DoubleDefinitionException, if there are multiple occurences of the same convertibletrait in the
   *        given list of triple sets
   * @param l the list to be checked for doubles
   */
  private def checkForDoubles( l : List[TripleSet] ) = l.foldLeft(Set[ConvertibleTrait[_]]()){
    (set, elem) => if (elem._3.forall(x => !set.contains(x))) set ++ elem._3 else
      throw DoubleDefinitionException(elem._3.filter(set.contains).mkString(", "))
  }

  /**
   *  updates the ownermap according to the information from the given list of triplesets
   * @param l the list containing the needed information
   */
  private def setOwners( l : List[TripleSet] ) {
    ownerMap = l.foldLeft(ownerMap){
      (m, c) => c._3.foldLeft(m){
        (oMap, e) => oMap.updated(e.sVarIdentifier, oMap.getOrElse(e.sVarIdentifier, c._1))
      }
    }
  }

  /**
   *  sends a GetInitialValuesMsg to the first actor in the given build order and stores its tail in the
   *        variable sortedDeps
   * @param buildorder the buildorder to be processed
   */
  private def requestInitialValues( buildorder : List[TripleSet] ) {
    buildorder match {
      case Nil => createSVars()
      case (actor, aspect, providings) :: tail =>
        sortedDeps = tail
        actor ! GetInitialValuesMsg( Actor.self, providings.toSet, aspect, theEntity, values )
    }
  }

  //process initial values
  addHandler[GetInitialValuesAns]{
    msg : GetInitialValuesAns =>
      values ++= msg.initialValues
      requestInitialValues(sortedDeps)
  }

  //actually create the svars
  /**
   *  generates a list of actor provideconversion-set tuples. the actor in the head tuple of this list will be
   *        sent an CreateSVarsMsg (containing the entity to be filled) that will be passed around. Actor.self will be
   *        the last receiver of that message
   */
  private def createSVars() {
    val providings = storedDeps.values.flatMap(_.flatMap(_._2.flatMap(_.providings.objects)))
    checkValidity(providings.toSeq, values.map(_.typedSemantics))
    providings.foldLeft(Map[Actor, Set[ProvideConversionInfo[_,_]]]()){
      (map, p) => getOrElseUpdate(map, ownerMap(p.sVarIdentifier), combineWithValue(p, values))
    }.toList :+ (Actor.self -> Set[ProvideConversionInfo[_,_]]()) match {
      case (actor, set) :: tail => actor ! CreateSVarsMsg(set, theEntity, tail)
      case Nil  => // this will never happen as we added Actor.self to the list
    }
  }

  /**
   *  checks if all SVars that the aspects require will be provided. Throws an Exception if this is not the case
   * @params requirings the given requirings
   * @params providings the given providings
   */
  private def checkValidity( requirings : Seq[ConvertibleTrait[_]], providings : Seq[ConvertibleTrait[_]] ) {
    val missing = requirings.filter( p => providings.find( _.sVarIdentifier == p.sVarIdentifier ).isEmpty )
    if (missing.nonEmpty) throw NoInitialValuesException(missing, ownerMap, entityAspects)
  }

  addHandler[CreateSVarsMsg]{ msg : CreateSVarsMsg =>
    val entity = createType(msg.entity.seal.asInstanceOf[Entity with Removability])
    entityAspects.foreach{ asp =>
      Component(asp.componentType).foreach{ c =>
        c ! EntityCompleteMsg(Actor.self, asp, entity)
        openCreateNotifications += c
      }
    }

  }

  addHandler[EntityConfigCompleted] {
    msg : EntityConfigCompleted =>
      openCreateNotifications.remove(msg.sender)
      if( openCreateNotifications.isEmpty ) {
        msg.e.addRemoveObserver(Actor.self)
        myHandler(msg.e.asInstanceOf[Type with Removability], entityAspects )
        // ToDO: This is quite bad, because the handler function is called in the thread of the EntityConfig Actor
        // ToDO: Not by the actor that registred the handler! This could lead to a concurrent access to the same
        // ToDO: And race conditions.
        theHandler.collect{ case h => h( msg.e.asInstanceOf[Type with Removability] ) }
      }
  }

  addHandler[RemoveEntityMessage]{
    msg => SVarActorImpl.self.shutdown()
  }


  //helpers
  /**
   *  adds the given value to the set which is stored in map under the given key. If that set did not exist, it
   *        is created
   * @param map the map to be filled
   * @param key the key to access the set
   * @param value the value to be inserted into the set
   * @return the updated map
   */
  private def getOrElseUpdate[K, V]( map : Map[K, Set[V]], key : K, value : V) =
    map.updated(key, map.getOrElse(key, Set[V]()) + value)

  /**
   *  calls the withInitialValue function on the ProvideConversionInfo generated by the given ConvertibleTrait if
   *        there is a value provided for the convertible trait. returns the generated ProvideConversionInfo
   * @param c the ConvertibleTrait to be processed
   */
  private def combineWithValue[T](c : ConvertibleTrait[T], cset : SValList) : ProvideConversionInfo[T,_] =
    cset.getFirstValueFor(c) match {
      case Some(value) => c.isProvided.withInitialValue(value).wrapped
      case None => c.isProvided.wrapped
    }
}

case class ResolveRequirementsException( aspects : String )
  extends Exception("Could not resolve requirements for aspects:\n\t" + aspects.mkString("\n\t"))

case class DoubleDefinitionException( doubles : String )
  extends Exception("The following SVars are at least defined twice:\n\t" + doubles)

case class InvalidEntityDescriptionException( ed : IEntityDescription[Entity], reason : String )
  extends Exception("Invalid EntityDescription " + ed + ":\n" + reason)

case class NoInitialValuesException(m : Seq[ConvertibleTrait[_]], ownerMap : Map[Symbol, Actor], as : Seq[EntityAspect])
  extends Exception( m.foldLeft("The following values are never provided:"){
    (str, elem) => str + "\n\t" + elem + " (owner: " + ownerMap.get(elem.sVarIdentifier).collect{
      case c : Component => c.componentName.name + ", semantics: " +
        as.find(_.componentType.equals(c.componentType)).collect{
          case a => a.createParamSet.semantics.toString
        }.getOrElse("unknown")
      case a => a.toString
    }.getOrElse("unknown") +")"
  } )