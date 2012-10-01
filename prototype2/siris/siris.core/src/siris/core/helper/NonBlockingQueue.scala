package siris.core.helper

import actors.Actor

/* author: dwiebusch
 * date: 02.10.2010
 */


/**
 * The actual Non-BlockingQueue. Again, I am aware that one could implement this with
 * just head and tail, but using the root element you can distinguish between two en-/
 * dequeuing runs.
 *
 * However this is subject to discussions
 */
private[helper] class NonBlockingQueue[T](idSource : IdProvider, reader : Actor, writer : Actor){
  private final val root  = new IElement[T](None, 0)
  private val memPool     = new MemPool[T]

  //this var belongs to the adding thread
  private var lastAccessed = root
  //this var belongs to the taking thread
  private var lastTaken    = root

  def !( msg : T ) : NonBlockingQueue[T] = {
    if (Actor.self != writer)
      throw new Exception("Not allowed to write")
    addElement( msg )
    this
  }

  /**
   * if the root.nextAccessible variable is true, this means the taking actor still has not started to work, therefore
   * we have to append to the queues end. Otherwise, the taking actor has passed the first element and we can start a
   * new queue. We do this by setting the roots next element, making it accessible and setting the nextAccessible of the
   * queue's end to true, therefore the taking actor will recognize the last element
   */
  protected def addElement( value : T ) {
    val newElement = memPool.take( value, idSource.nextId )
    if (root.nextIsAccessible.get)
      lastAccessed.next = Some(newElement)
    else
      root.setNext(newElement)
    lastAccessed.nextIsAccessible.set(true)
    lastAccessed = newElement
  }

  /**
   * if the lastTaken element has an successor (the Some(element) case) we add the previously taken element to the
   * mempool and get the successors value. if there is no successor, we reset the queue to the new root but return null,
   * therefore the requesting actor will be informed to process other queues before dequeuing elements from this one
   * again
   */
  def take() : Option[T] =
    if ( Actor.self != reader )
      throw new Exception("Not allowed to read")
    else if (lastTaken.nextIsAccessible.get)
      lastTaken.next match {
        case Some(element) =>
          if ( lastTaken != root )
            memPool returnToPool lastTaken
          else
            root.nextIsAccessible.set(false)
          lastTaken = element
          element.value
        case None =>
          memPool returnToPool lastTaken
          lastTaken = root
          take()
      }
    else  None

  def peek : Int = {
    if (lastTaken.nextIsAccessible.get)
      lastTaken.next.collect{ case el => return el.count }
    IdProvider.NOTHING_FOUND
  }
}