package siris.core.helper

import actors.Actor
import scala.collection.mutable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * untested yet
 *
 * i am aware of the existence of java.util.concurrent.atomic.AtomicMarkableReference, but
 * as in this case the application is quite straightforward, i think one should use as few
 * atomic operations as possible, since in the javadoc of java.util.concurrent.atomic is
 * written:
 *
 * "However on some platforms, support may entail some form of internal locking."
 *
 * @author dwiebusch
 * date: 24.09.2010
 */

class ActorMessageQueue extends IdProvider{
  private type QType = mutable.PriorityQueue[(Int, Any)]

  private var lastN = 0
  private val root  = new Element(Actor.self, this)
  private val processingQueue = new QType()( new Ordering[(Int, Any)]{
    def compare(x: (Int, Any), y: (Int, Any)) = y._1 - x._1
  })

  def !( value : Any) : NonBlockingQueue[Any] =
    getOrAddQueue( Actor.self ) ! value

  def apply( handler : Any => Unit ) {
    while ( fillQueue( processingQueue, lastN + 10 ) )
      processingQueue.foreach{ x => lastN = (x._1, handler apply x._2)._1 }
  }

  private def fillQueue(q : QType, startingId : Int ) : Boolean = {
    def fillQueue( queue : QType, startingId : Int, nextId : Int, currentElement : Element = root) : QType =
      if (nextId < startingId + queue.length && nextId != IdProvider.NOTHING_FOUND){
        queue.enqueue( nextId -> currentElement.queue.take().get )
        fillQueue( queue, startingId, currentElement.queue.peek, currentElement )
      }
      else if (currentElement.nextIsAccessible.get) currentElement.next match {
        case Some(nextElement) => fillQueue(queue, startingId, nextElement.queue.peek, nextElement)
        case None => queue
      }
      else
        queue

    q.clear()
    fillQueue(q, startingId, root.queue.peek, root).nonEmpty
  }

  //we don't want to lock the whole class here but only the last line, so synchronize might be a bit to much
  private def getOrAddQueue( a : Actor, e : Element = root ) : NonBlockingQueue[Any] = {
    if (e.nextIsAccessible.get)
      e.next collect { case s => return if (s.id == a) s.queue else getOrAddQueue(a, s) }
    synchronized{ e.addSuccessor( a ) }.queue
  }
}

object IdProvider{
  final val NOTHING_FOUND = -1
}
/**
 * we don't care if some id is made availabe twice, since we only need lastId >= newId, therefore we don't sync here
 */
protected trait IdProvider{
  private var id : Int = 0

  protected[helper] def nextId : Int = {
    id += 1
    id -  1
  }
}

//little helper class
private class Element( val id : Actor, owner : ActorMessageQueue ){
  val queue = new NonBlockingQueue[Any](owner, Actor.self, id)
  var next  : Option[Element] = None
  var nextIsAccessible = new AtomicBoolean(true)

  def addSuccessor( id : Actor ) : Element = next match {
    case Some(successor) => successor.addSuccessor(id)
    case None =>
      nextIsAccessible.set(false)
      next = Some(new Element(id, owner))
      nextIsAccessible.set(true)
      next.get
  }
}
