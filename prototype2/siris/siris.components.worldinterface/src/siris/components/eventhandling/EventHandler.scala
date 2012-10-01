package siris.components.eventhandling

import siris.core.svaractor.handlersupport.HandlerSupport
import scala.collection.mutable
import siris.components.worldinterface.WorldInterface
import actors.Actor
import siris.core.entity.description.{SVal, Semantics}

/* author: dwiebusch
* date: 10.09.2010
*/


trait EventHandler extends Actor with HandlerSupport{
  def handleEvent(e : Event)

  def requestEvent[T]( event : EventDescription ) {
    WorldInterface.requireEvent(this, event)
  }

  addHandler[EventProviderMessage]{
    (msg : EventProviderMessage) => msg.provider.foreach{ _ ! RegisterEventHandlerMessage(this, msg.event) }
  }

  addHandler[Event]{
    handleEvent(_)
  }
}

trait EventProvider extends Actor with HandlerSupport {
  private type HRPair = (EventHandler, PartialFunction[Event, Boolean])
  protected val eventHandlers =  mutable.Map[SVal[Semantics], Set[HRPair]]()

  protected def requireEvent( handler : EventHandler, event : EventDescription ) {
    eventHandlers.update(event.name, eventHandlers.get(event.name) match {
      case None => Set(handler -> event.restriction.orElse{case _ => false})
      case Some(set) => set + (set.find( _._1 == handler ) match {
        case Some((_, func)) => handler -> event.restriction.orElse(func)
        case None => handler -> event.restriction.orElse{case _ => false}
      })
    })
  }

  def provideEvent( e : EventDescription ) {
    WorldInterface.provideEvent(this, e)
  }

  def removeEventHandler(handler: EventHandler, event : Option[EventDescription] = None) { event match {
    case Some(e) =>
      eventHandlers.update(e.name, eventHandlers.getOrElse(e.name, Set[HRPair]()).filterNot(filterFunc(_, handler)))
    case None => for ( (event, handlers) <- eventHandlers)
      eventHandlers.update(event, handlers.filterNot(filterFunc(_, handler )))
  } }

  private def filterFunc( pair : HRPair, handler : EventHandler ) : Boolean =
    pair._1 == handler

  def emitEvent( e : Event ) = eventHandlers get e.name collect {
    case s => s.foreach{ pair => if ( pair._2(e) && filter(pair._1, e) ) pair._1 ! e }
  }

  def filter(handler : EventHandler, e : Event) : Boolean =
    true

  addHandler[RegisterEventHandlerMessage]{
    (msg : RegisterEventHandlerMessage) => requireEvent( msg.handler, msg.event )
  }
}

private[components] case class EventProviderMessage( provider : Set[EventProvider], event : EventDescription )
private[components] case class RegisterEventHandlerMessage( handler : EventHandler, event : EventDescription )
private[components] case class RegistrationOK( provider : EventProvider, event : EventDescription )
