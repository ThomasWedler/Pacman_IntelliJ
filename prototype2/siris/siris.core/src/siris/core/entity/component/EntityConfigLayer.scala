package siris.core.entity.component

import actors.Actor
import scala.collection.mutable
import siris.core.entity.description._
import siris.core.entity.typeconversion._
import siris.core.entity.{SVarContainer, Entity}
import siris.core.svaractor.{SVarActorImpl, SVarActor, SVar}
import siris.core.svaractor.handlersupport.HandlerSupport

/* author: dwiebusch
 * date: 02.09.2010
 */

//! Message to inform Actors about new created Entities
case class EntityCreatedMessage( e : Entity with Removability )

//! request for dependencies
protected[entity] case class GetDependenciesMsg( sender : Actor, asp : EntityAspect )
//! answer to request for dependencies
protected[entity] case class GetDependenciesAns( sender : SVarActor, deps : Set[Dependencies], asp : EntityAspect )

//! request for initial values
// ToDO: Document
case class GetInitialValuesMsg( sender : Actor, p : Set[ConvertibleTrait[_]], asp : EntityAspect,
                                e : Entity, given : SValList )
//! answer to request for initial values
// TODO: Document
case class GetInitialValuesAns( sender : SVarActor, initialValues : SValList )

//! request to create svars
protected[entity] case class CreateSVarsMsg( toCreate : Set[ProvideConversionInfo[_,_]],
                                             entity : Entity with Removability with Mutability,
                                             queue : List[(Actor, Set[ProvideConversionInfo[_,_]])] )

//! information about a new entity
// TODO: Document
case class EntityCompleteMsg( sender: Actor, asp : EntityAspect, e : Entity with Removability)

//! message to inform an actor about the removal of an entity
protected[entity] case class RemoveEntityMessage( sender : Actor, e : Entity )

// TODO: Document
case class EntityConfigCompleted( sender : Actor, e : Entity with Removability )
/**
 *  the trait to be mixed in by all components that participate in the entity creation process
 */
trait LowerEntityConfigLayer extends HandlerSupport{
  //! the set of known removable entities
  private val knownRemovableEntities = mutable.Map[Entity, Removability]()
  //! the set of convertible hints
  private val convertibleHints = mutable.Map[Symbol, ConvertibleTrait[_]]()

  //! the components name (used for lookup functionality)
  def componentName : Symbol
  //! the components type (used for lookup functionality)
  def componentType : SVal[Semantics]

  /**
   *  returns a set of additional convertibletraits specifying the addidional svars provided by this component
   * @return the set
   */
  protected def getAdditionalProvidings( aspect : EntityAspect ) : Set[ConvertibleTrait[_]] = Set()

  /**
   *  returns the set of dependencies that need to be met for this component to provide particular initial values
   * @param aspect the aspect the be realized
   * @return the set
   */
  protected def getDependencies( aspect : EntityAspect ) : Set[Dependencies] = {
    val retVal = aspect.getRequirings.foldLeft(Set[Dependencies]()){ (a, b) => a + Dependencies(Requiring(b)) }
    aspect.getProvidings.foldLeft(retVal){ (a, b) => a + Dependencies(Providing(b))}
  }

  /**
   *  method to be implemented by each component. Will be called when an entity has to be removed from the
   * internal representation.
   * @param e the Entity to be removed
   */
  protected def removeFromLocalRep(e : Entity)

  //add handler to answer get dependency messages
  addHandler[GetDependenciesMsg]{ msg : GetDependenciesMsg =>
    val additionalPs = getAdditionalProvidings(msg.asp)
    var deps = getDependencies(msg.asp + additionalPs)
    val toAdd = additionalPs.filter{p => deps.find(_.providings.objects.contains(p)).isEmpty }
    if (toAdd.nonEmpty) deps = deps + Dependencies(Providing(toAdd.toSeq : _*))
    msg.sender ! GetDependenciesAns( SVarActorImpl.self, deps, msg.asp )
  }

  //add handler to react to CreateSVarsMsgs
  addHandler[CreateSVarsMsg]{ msg : CreateSVarsMsg =>
    knownRemovableEntities += msg.entity -> msg.entity
    msg.toCreate.foreach{ _.injectSVar(msg.entity) }
    msg.queue.headOption.collect{
      case (actor, set) => actor ! CreateSVarsMsg(set, msg.entity, msg.queue.tail)
    } : Unit
  }

  // message handler to support remove entity messages
  addHandler[RemoveEntityMessage]{ msg : RemoveEntityMessage => removeFromLocalRep( msg.e ) }

  /**
   * initiates removal of an entity. Therefore it has to be known by this instance
   * @param e the entity to be removed
   */
  protected def removeEntity( e : Entity ) {
    knownRemovableEntities.remove(e) match {
      case Some(thing) => thing.remove()
      case None => e match {
        case removable : Removability => removable.remove()
        case _ => println("tried to remove entity" + e + ", which is not removable")
      }
    }
  }

  /**
   *  registers a convertible hint. When using  (o : OntologyMember).isRequired within aspects for this component,
   * the component will instanciate a svar of type T instead of the internal type
   * @param c the convertible hint (represented by an convertible trait) to be registered
   */
  def registerConvertibleHint[T]( c : ConvertibleTrait[T] ) =
    convertibleHints += c.sVarIdentifier -> c
}

trait EntityConfigLayer extends LowerEntityConfigLayer {
  //! the map of unanswered create requests
  private val openCreateRequests = mutable.Map[Entity, (Actor, Set[ConvertibleTrait[_]])]()

  /**
   * calls provideInitialValues with the full set of initial values
   * @param toProvide the convertibletraits for which values shall be provided
   * @param aspect the aspect providing the context for this method call
   * @param e the entity to be filled
   * @param given a set of create parameters that were already provided
   *
   */
  protected def requestInitialValues( toProvide : Set[ConvertibleTrait[_]], aspect : EntityAspect,
                                      e : Entity, given : SValList )

  /**
   *  used to integrate the entity into the local representation
   * @param e the entity to be integrated
   */
  protected def entityConfigComplete( e : Entity with Removability, aspect : EntityAspect )

  /**
   * to be called to inform the entity creation actor of the new initial values
   * @param e the entity for which initial values shall be provided
   * @param providings the list of provided values
   */
  protected def provideInitialValues( e : Entity, providings : SValList ) {
    val (creator, toProvide) = openCreateRequests.getOrElse(e, throw new Exception("Unknown entity: " + e))
    val open = toProvide.filterNot(providings.map(_.typedSemantics).contains)
    if (open.nonEmpty) throw new Exception("Error: " + open.mkString(" and ") + " was/were not provided")
    creator ! GetInitialValuesAns(SVarActorImpl.self, providings)
    openCreateRequests.remove(e)
  }

  //add handler to answer get initial value messages
  addHandler[GetInitialValuesMsg]{ msg : GetInitialValuesMsg =>
    openCreateRequests.update( msg.e, (msg.sender, msg.p) )
    requestInitialValues( msg.p, msg.asp, msg.e, msg.given )
  }

  //add handler react to EntityCompleteMsgs
  addHandler[EntityCompleteMsg]{ msg : EntityCompleteMsg =>
    entityConfigComplete(msg.e, msg.asp)
    msg.e.addRemoveObserver(Actor.self)
    msg.sender ! EntityConfigCompleted( Actor.self, msg.e )
  }
}


/**
 * helper object to create entities with removability trait
 */
private object Removability{
  def apply( e : Entity, os : Iterable[Actor] ) : Entity with Removability =
    os.foldLeft(new Entity(e) with Removability)((entity, observer) => {entity.addRemoveObserver(observer); entity})
}

/**
 * Removability trait which extends entities with a remove methor
 */
trait Removability extends Entity {
  //! the list of observers to be notified on removal of this enity
  protected val observers = mutable.Set[Actor]()

  //! Returns the list of observers to be notified on removal of this enity
  def getObservers =
    observers.toSet

  /**
   *  adds an observer which is notified via a RemoveEntityMessage when this entity is removed
   * @param observer the observer to be added
   */
  def addRemoveObserver( observer :  Actor ) =
    observers += observer

  /**
   *  initializes the removal of this entity. This means that all observers as well as the observers registered
   * with the EntityDescription object are sent a RemoveEntityMessage
   */
  def remove() {
    IEntityDescription.propagateEntityRemoval( this )
    observers.foreach( _ ! RemoveEntityMessage( Actor.self, this ) )
  }
}


/**
 * Message to start the entity creation process
 */
protected[entity] case class CreateEntity(buildOrder : List[(Symbol, NamedSValList, ProvideAndRequire)],
                                          handler : Entity with Removability => Any,
                                          toNotify : List[(Actor, NamedSValList)] = Nil)

/**
 * Trait that adds mutability to entities (their map "becomes mutable")
 * For internal use only
 */
protected trait Mutability{
  //! the mutable map
  protected var mutableMap = mutable.HashMap[Symbol, List[SVarContainer[_]]]()

  /**
   * creates a clone of this entity
   * @return a clone of this entity
   */
  private[core] def createClone : Entity with Mutability

  /**
   * merges two mutable entities (actually merges the local map with the other entity's map)
   * @param toMerge the other entity
   */
  private[core] def mergeWith( toMerge : Mutability ) {
    toMerge.mutableMap.foreach(update)
  }

  protected def update( p : Tuple2[Symbol, List[SVarContainer[_]] ] ) {
    mutableMap.update(p._1, p._2 ::: mutableMap.getOrElse(p._1, Nil))
  }

  /**
   * creates an immutable entity from this entity
   * @return the immutable copy of this entity
   */
  def seal : Entity
}

/**
 * implementation of the Mutability trait
 * For internal use only
 */
protected[entity] trait PluggableMutability extends Entity with Mutability {

  /**
   *  Returns the sVarNames of all SVars of this Entity
   */
  override def getAllSVarNames = mutableMap.keySet.toSet

  /**
   * creates a clone of this mutable entity
   * @return a clone of this mutable entity
   */
  private[core] def createClone : Entity with Mutability = {
    val retVal = new Entity(Map[Symbol, List[SVarContainer[_]]](), id) with PluggableMutability
    retVal.mutableMap = mutableMap.clone()
    retVal
  }

  /**
   * redirects injected svars to the mutable map (instead of the mutable map)
   * @param sVarName to use within the injection process
   * @param sVar the SVar to be inserted
   * @param info the convertible trait representing the svars type
   * @return this entity
   */
  override def injectSVar[T](sVarName: Symbol, sVar: SVar[T], info: ConvertibleTrait[T],
                             annotations : Set[SVal[Semantics]] = Set()) : Entity = {
    update( (sVarName, new SVarContainer( sVar, info, annotations ) :: Nil ))
    this
  }

  /**
   * redirects the access method to the mutable map
   * @param sVarName the symbol used to access the svar
   * @return the SVarContainerBase which contains the requested svar and its type information
   */
  override protected def access(sVarName: Symbol) : List[SVarContainer[_]] =
    mutableMap.get(sVarName).getOrElse(Nil)

  /**
   * seals this entity (makes it immutable)
   * @return a immutable copy of this entity
   */
  def seal : Entity = this match {
    case e : Removability =>
      new Entity(mutableMap.toMap, id) with Removability{ override val observers = mutable.Set(e.getObservers.toSeq:_*) }
    case e : Entity =>
      new Entity(mutableMap.toMap, id)
  }
}
