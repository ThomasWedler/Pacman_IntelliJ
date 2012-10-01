package siris.components.eventhandling

/**
 * Created by IntelliJ IDEA.
 * User: dwiebusch
 * Date: 11.02.11
 * Time: 14:43
 */

trait EventHandlerAdding extends EventHandler{
  protected var handlers = Map[EventDescription, Event => Unit]()

  def handleEvent(e: Event) {
    handlers.foreach( h => if (h._1.matches(e)) h._2.apply(e) )
  }

  def addHandler( desc : EventDescription, handler : Event => Unit) {
    handlers = handlers + (desc -> handler)
  }

  def removeHandler( desc : EventDescription ) {
    handlers = handlers.filterNot( _._1 == desc )
  }
}