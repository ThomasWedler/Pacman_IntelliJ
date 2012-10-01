package siris.components.worldinterface

import actors.Actor
import scala.collection.mutable
import siris.core.entity.Entity
import siris.core.component.Component
import siris.core.svaractor._
import handlersupport.Types
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.components.eventhandling._
import siris.core.entity.description.{SVal, Semantics}

/* author: dwiebusch
* date: 10.09.2010
*/

/**
 * The World Interface Actor, which is doing all the Interfacing work
 *
 * @author dwiebusch
 *
 */
class WorldInterfaceActor extends SVarActorHW with EventProvider {
  /** the world root, this is some kind of hack for now*/
  private val worldRoot = new RecursiveHolder
  /** an internal list to store handlers for every registered event (and the internal handling stuff) */
  //private var handlers   = List[Types.handler_t]()
  /** an internal map, containing all triggered world interface events (or at least their names) */
  //private var triggers   = Map[SVar[_], (Symbol, Symbol)]()
  /** the map containing all registered actors */
  private var actors     = Map[Symbol, Actor]()
  /** the map containing all registered components */
  private var components = Map[Symbol, Component]()
  /** map of creation observers */
  private val creationObservers = mutable.Map[List[Symbol], Set[Actor]]()

  private val eventProviders = mutable.Map[SVal[Semantics], Set[EventProvider]]()
  case class InvalidValueTypeException(val reason : String) extends Exception

  /**
   * adds an entity to the world root
   *
   * @param desc the entitys name
   * @param e an entity (if None, a new one will be created)
   * @param holder you don't want to change the default value
   */
  private def setEntity( desc : List[Symbol], e : Option[Entity] = None,
                         holder : RecursiveHolder = worldRoot, path : List[Symbol] = Nil) {
    desc match{
      case name :: Nil  =>
        holder.items.update( name, e.getOrElse(new Entity) )
        creationObservers.filter( x => (name::path).reverse.startsWith(x._1) ).values.foreach{ v =>
          v.foreach( _ ! CreationMessage((name::path).reverse, holder.items.get(name).get) )
        }
      case head :: tail => setEntity( tail, e, holder.children.getOrElseUpdate(head, new RecursiveHolder), head :: path)
      case Nil => throw new Exception("provided empty list, cannot insert entity")
    }
  }

  /**
   * retrieves an entity from the world root
   *
   * @param desc the name of the entity to be retrieved
   * @param holder you don't want to change the default value 
   * @return the entity to be retrieved, or none if no entity was registered under the given name
   */

  private def getEntity( desc : List[Symbol], holder : RecursiveHolder = worldRoot ) : Option[Entity] = desc match {
    case name :: Nil  => holder.items.get(name)
    case head :: tail => getEntity(tail, holder.children.apply(head))
    case Nil => throw new Exception("provided empty list, cannot retreive entity")
  }


  private def getEntitiesBelow( desc : List[Symbol], holder : RecursiveHolder = worldRoot ) : Option[Set[Entity]] = desc match {
    case Nil => Some(holder.getItemsRecursively)
    case head :: tail => holder.children.get(head) match {
      case None if (tail == Nil) => Some(holder.items.get(head).toSet)
      case Some(x) => getEntitiesBelow(tail, x )
      case None => None
    }
  }

  override protected def requireEvent(handler: EventHandler, event: EventDescription ) {
    super.requireEvent(handler, event)
    eventProviders.get(event.name) collect {
      case set => handler ! EventProviderMessage(set, event)
    }
  }

  /**
   * updates an entity by injecting the given state value
   *
   * @param name the state value's name used in the injection process
   * @param stateValue the state value to be injected
   * @param container name the name of the entity into which the state value shall be injected
   * (a new one is created if no one is known under the given name)
   */
  private def addStateValue[T](stateValue : SVar[T], desc : ConvertibleTrait[T], containerName : List[Symbol]) {
    setEntity(containerName, Some(getEntity(containerName).getOrElse(new Entity).injectSVar(stateValue, desc)) )
  }

  /**
   * adds a state value to be observed, triggering a WorldInterfaceEvent with the given name
   *
   * @param stateValue the state value to be observed
   * @param trigger the name of the WorldInterfaceEvent to be triggered on value changes of stateValue
   */
  private def addValueChangeTrigger[T](stateValue : SVar[T], trigger : Symbol, container : List[Symbol]) {
    stateValue.observe( value => emitEvent(WorldInterfaceEvent(trigger, (stateValue, container, value ) ) ) )
  }

  def addValueChangeTrigger[T](stateValue: ConvertibleTrait[T], entityName: List[Symbol], handlerName: Symbol) {
    getEntity(entityName).collect{
      case e => e.get(stateValue).collect{ case svar => addValueChangeTrigger(svar, handlerName, entityName) }
    }
  }


  /**
   * handles a message. if this is called from the WorldInterfaceActor's thread, the handling is executed instantaneously
   * otherwise the message is forwarded to the WorldInterfaceActor
   */
  private[worldinterface] def handleMessage(msg : Any) {
    if (Actor.self == this) applyHandlers(msg) else this ! msg
  }

  /**
   * checks the known handlers if they can handle the event and handles a given event if a matching handler was found
   * otherwise calls the catchAll handler
   *
   * @param handlers a list of handlers to be checked
   * @param catchAll the handler to be returned if no handler contained in the list matches
   * @return a handler that can handle the given event or catchAll if no such handler was found in the given list
   */
  private def handleEvent(handlers : List[Types.handler_t], catchAll : Types.handler_t): Types.handler_t =
    handlers match {
      case head :: tail => head orElse handleEvent(tail, catchAll)
      case Nil => catchAll
    }

  addHandler[ListenForRegistrationsMessage]{ msg =>
    creationObservers.update(msg.path, creationObservers.getOrElse(msg.path, Set[Actor]()) + msg.actor)
    getEntitiesBelow(msg.path).collect{ case set => set.foreach( msg.actor ! CreationMessage(msg.path, _) ) }
  }

  addHandler[RegisterHandlerMessage]{
    msg => requireEvent( msg.handler, msg.event )
  }

  addHandler[UnRegisterHandlerMessage]{
    msg => removeEventHandler( msg.handler, msg.e )
  }

  addHandler[ProvideEventMessage]{ msg =>
    eventProviders.update( msg.event.name, eventProviders.getOrElse(msg.event.name, Set[EventProvider]()) + msg.provider )
    eventHandlers.get(msg.event.name) collect {
      case set => set.foreach{ p => p._1 ! EventProviderMessage(Set(msg.provider), msg.event)}
    }
  }

  addHandler[UnRegisterProviderMessage]{ msg =>
    msg.e match {
      case Some(event) =>
        eventProviders.update(event.name, eventProviders.getOrElse(event.name, Set[EventProvider]()).filterNot( _ == msg.provider ))
      case None => for ( (event, providers) <- eventProviders)
        eventProviders.update(event, providers.filterNot( _ == msg.provider))
    }
  }

  addHandler[ForwardMessageRequest]{
    msg => actors get msg.destination match{
      case Some(dst) => dst ! msg.msg
      case None =>
    }
  }

  addHandler[ActorRegisterRequest]{
    msg =>  actors += msg.name -> msg.actor
  }

  addHandler[ActorListingRequest]{
    msg => msg.replyTo ! ActorListingReply(actors.keys.toList)
  }

  addHandler[ComponentRegisterRequest]{
    msg => components += msg.name -> msg.component
  }

  addHandler[EntityCreateRequest]{
    msg => setEntity(msg.name)
  }

  addHandler[EntityRegisterRequest]{
    msg => setEntity(msg.name, Some(msg.e) )
  }

  addHandler[StateValueCreateRequest[_]]{
    case msg: StateValueCreateRequest[_] => addStateValue(SVarImpl(msg.value), msg.desc, msg.container)
  }

  addHandler[ExternalStateValueObserveRequest[_]]{
    msg => addValueChangeTrigger(msg.ovalue, msg.trigger, msg.container)
  }

  addHandler[ActorEnumerateRequest]{
    msg => msg.handler(Some(actors.keys.toList) )
  }

  addHandler[ComponentLookupRequest]{
    msg => msg.handler( components.get(msg.name) )
  }

  addHandler[AllComponentsLookupRequest]{
    msg => msg.handler( components )
  }

  addHandler[EntityLookupRequest]{
    msg => msg.handler( getEntity(msg.name) )
  }

  addHandler[ActorLookupRequest]{
    msg => msg.handler( actors.get(msg.name) )
  }

  addHandler[EntityUnregisterRequest]{
    msg => worldRoot.remove(msg.e)
  }

  addHandler[InternalStateValueObserveRequest[_]]{
    msg => getEntity(msg.nameE).collect{
      case entity => entity.get(msg.c).collect{ case svar =>addValueChangeTrigger(svar, msg.trigger, msg.nameE) }
    }
  }

  addHandler[StateValueSetRequest[_]]{
    case msg: StateValueSetRequest[_] => getEntity(msg.container).collect{
      case entity => entity.get(msg.c).get.set(msg.newValue)
    }
  }

  addHandler[EntityGroupLookupRequest]{
    msg => msg.handler(getEntitiesBelow(msg.name))
  }

  addHandler[StateValueLookupRequest[_]]{
    case StateValueLookupRequest(c, container, handler) => getEntity(container) match {
      case Some(entity) => handler( entity.get(c) )
      case None => handler( None )
    }
  }

  addHandler[ReadRequest[_]]{
    case ReadRequest(svar, handler) => svar.get{ handler(_) }
  }

  addHandler[SVarReadRequest[_]]{
    case SVarReadRequest(c, entity, handler) => getEntity(entity) match {
      case Some(e) => e.get(c) match {
        case Some(svar) => svar.get{ value => handler(Some(value) ) }
        case None => handler(None)
      }
      case None => handler(None)
    }
  }
}


class RecursiveHolder{
  val items = mutable.Map[Symbol, Entity]()
  val children = mutable.Map[Symbol, RecursiveHolder]()

  def getItemsRecursively : Set[Entity] =
    children.foldLeft(items.values.toSet)( (a, b) => a ++ b._2.getItemsRecursively )

  def remove(e : Entity) {
    items.retain( (a,b) => b != e )
    children.values.foreach( _.remove(e) )
  }
}