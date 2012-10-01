package siris.components.worldinterface

import actors.Actor
import siris.core.entity.Entity
import siris.core.component.Component
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.components.eventhandling.{EventDescription, EventProvider, EventHandler, Event}
import siris.core.entity.description.SValList
import siris.ontology.Symbols
import siris.core.svaractor.{SVarActor, SVar}
import java.util.UUID
import java.lang.Exception

/* author: dwiebusch
* date: 10.09.2010
*/


/**
 * Base Class for events handled by the WorldInterfaceActor
 *
 * @author dwiebusch
 * @param name the event's name
 * @param data data accompanying the event
 */
case class WorldInterfaceEvent( val specificName : Symbol, val value : Any)
  extends Event( Symbols.event, new SValList())

/**
 * Message sent to the WorldInterface Actor, to register a new handler for the given event
 *
 * @author dwiebusch
 * @param handler the handler to be registered
 */
case class RegisterHandlerMessage( handler : EventHandler, event : EventDescription )


case class ProvideEventMessage(provider : EventProvider, event : EventDescription )

protected class HandlerMSG[T]( handler : T => Any ) {
  private case class Reply[T](id : UUID, value : T)
  private var sender : Option[Actor] = None
  private val id = UUID.randomUUID()

  def registerHandlerAndSend( requester : SVarActor, receiver : Actor ) {
    requester.addSingleUseHandler[Reply[T]]{ case Reply(`id`, v) => handler(v) }
    sender = Some(requester)
    receiver ! this
  }

  def reply( value : T ) {
    sender.getOrElse( throw new Exception( "No Handler was registered" ) ) ! Reply(id, value)
  }
}

case class TestHandlerMSG(handler : Int => Unit) extends HandlerMSG(handler)

/**
 * Message sent to the WorldInterface Actor, to unregister a handler
 *
 * @author dwiebusch
 * @param handler the handler to be unregistered
 */
case class UnRegisterHandlerMessage( handler : EventHandler, e : Option[EventDescription] = None )

case class UnRegisterProviderMessage( provider : EventProvider, e : Option[EventDescription] = None )

/**
 * Message sent to the WorldInterface Actor, to create a new state value
 *
 * @author dwiebusch
 * @param valueID the state value's id, used for later reference
 * @param value the initial value of the created state value
 * @param container the id of the entity, the state value is injected into
 */
case class StateValueCreateRequest[T](desc: ConvertibleTrait[T], value: T, container: List[Symbol])


/**
 * Message sent to the WorldInterface Actor, to write to a registered state value
 *
 * @author dwiebusch
 * @param valueID the state value's id *
 * @param container the id of the entity which contains the state value
 * @param newValue the value to be set
 */
case class StateValueSetRequest[T](c : ConvertibleTrait[T], container : List[Symbol], newValue : T)

/**
 * Message sent to the WorldInterface Actor, to register a handler for a value change of the gieven state value
 *
 * @author dwiebusch
 * @param ovalue the state value to be observed
 * @param trigger the name of the WorldInterfaceEvent which is triggered on value change of ovalue
 */
case class ExternalStateValueObserveRequest[T](ovalue: SVar[T], container : List[Symbol], trigger: Symbol)

/**
 * Message sent to the WorldInterface Actor, to observe an state value, already registered with the WorldInterfaceActor
 *
 * @author dwiebusch
 * @param nameSV the state value's name
 * @param nameE the name of the entity containing the state value
 * @param trigger the name of the WorldInterfaceEvent which is triggered on value change of the state value
 */
case class InternalStateValueObserveRequest[T](c: ConvertibleTrait[T], nameE : List[Symbol], trigger: Symbol)

/**
 * Message sent to the WorldInterface Actor, to create a new entity
 *
 * @author dwiebusch
 * @param name the name of the entity to be created
 */
case class EntityCreateRequest(name: List[Symbol])

/**
 * Message sent to the WorldInterface Actor, to register an existing entity
 *
 * @author dwiebusch
 * @param name the name under which the entity will be accessible after being registered
 * @param e the entity to be registered
 */
case class EntityRegisterRequest(name : List[Symbol], e : Entity)
case class EntityUnregisterRequest(e : Entity)

/**
 * Message sent to the WorldInterface Actor, to register an existing actor
 *
 * @author dwiebusch
 * @param name the name under which the actor will be accessible after being registered
 * @param actor the actor to be registered
 */
case class ActorRegisterRequest(name : Symbol, actor : Actor)

/**
 * Message sent to the WorldInterface Actor, to receive a listing of all known actors
 *
 * @author dwiebusch
 * @param replyTo the actor to whom the list will be sent
 */
case class ActorListingRequest(replyTo : Actor)

/**
 * Message sent by the WorldInterface Actor, in reply to an ActorListingRequest
 *
 * @author dwiebusch
 * @param list the list containing the names of all actors known to the WorldInterfaceActor
 */
case class ActorListingReply(list : List[Symbol])

/**
 * Message sent to the WorldInterface Actor, to forward a message to another actor
 *
 * @author dwiebusch
 * @param destination the symbol under which the other actor was registered
 * @param msg the message to be forwarded
 */
case class ForwardMessageRequest(destination : Symbol, msg : Any)

/**
 * Message sent to the WorldInterface Actor, to register an existing component
 *
 * @author dwiebusch
 * @param name the name under which the component will be accessible after being registered
 * @param component the component to be registered
 */
case class ComponentRegisterRequest(name : Symbol, component : Component)


/**
 * Message sent to the WorldInterface Actor, receive a list of all actors known to the WorldInterfaceActor
 * FOR INTERNAL USAGE ONLY
 *
 * @author dwiebusch
 * @param future the future containing all known actors after the message was processed
 */
//private case class ActorEnumerateRequest(future : SyncVar[Option[List[Symbol]]])
private case class ActorEnumerateRequest(handler : Option[List[Symbol]] => Unit)

/**
 * Message sent to the WorldInterface Actor, to look up a specific actor
 * FOR INTERNAL USAGE ONLY
 *
 * @author dwiebusch
 * @param name the id of the actor to be looked up
 * @param future the future containing the actor after the message was processed
 */
//private case class ActorLookupRequest(name : Symbol, future : SyncVar[Option[Actor]])
private case class ActorLookupRequest(name : Symbol, handler : Option[Actor] => Unit)

/**
 * Message sent to the WorldInterface Actor,  to look up a specific entity
 * FOR INTERNAL USAGE ONLY
 *
 * @author dwiebusch
 * @param name the id of the entity to be looked up
 * @param future the future containing the looked up entity after the message was processed
 */
//private case class EntityLookupRequest(name : List[Symbol], future : SyncVar[Option[Entity]])
private case class EntityLookupRequest(name : List[Symbol], handler : Option[Entity] => Any)
//private case class EntityGroupLookupRequest(name : List[Symbol], future : SyncVar[Option[Set[Entity]]])
private case class EntityGroupLookupRequest(name : List[Symbol], handler : Option[Set[Entity]] => Any)

/**
 * Message sent to the WorldInterface Actor, to look up a specific component
 * FOR INTERNAL USAGE ONLY
 *
 * @author dwiebusch
 * @param name the name under which the component was registered
 * @param future the future containing the looked up component after the message was processed
 */
//private case class ComponentLookupRequest(name : Symbol, future : SyncVar[Option[Component]])
private case class ComponentLookupRequest(name : Symbol, handler : Option[Component] => Any)
//private case class AllComponentsLookupRequest(future : SyncVar[Map[Symbol, Component]])
private case class AllComponentsLookupRequest(handler : Map[Symbol, Component] => Unit)

/**
 * Message sent to the WorldInterface Actor, to look up a specific state value
 * FOR INTERNAL USAGE ONLY
 *
 * @author dwiebusch
 * @param name the state value's id
 * @param container the id of the entity containing the state value to be looked up
 * @param future the future containing a state value after the message was processed
 */
//private case class StateValueLookupRequest[T]( c : ConvertibleTrait[T], container : List[Symbol], future : SyncVar[Option[SVar[T]]])
private case class StateValueLookupRequest[T]( c : ConvertibleTrait[T], container : List[Symbol], handler : Option[SVar[T]] => Unit)

//private case class SVarReadRequest[T]( c : ConvertibleTrait[T], entity : List[Symbol], future : SyncVar[Option[T]] )
private case class SVarReadRequest[T]( c : ConvertibleTrait[T], entity : List[Symbol], handler : Option[T] => Unit )

//private case class ReadRequest[T]( svar : SVar[T], future : SyncVar[T] )
private case class ReadRequest[T]( svar : SVar[T], handler : T => Unit )

private case class ListenForRegistrationsMessage( actor : Actor, path : List[Symbol] )

case class CreationMessage( path : List[Symbol], e : Entity )

