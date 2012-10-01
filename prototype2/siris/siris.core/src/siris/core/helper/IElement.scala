package siris.core.helper

import java.util.concurrent.atomic.AtomicBoolean

/* author: dwiebusch
 * date: 02.10.2010
 */

// this would have been easier with AtomicMarkableReferences, see reasons in ActorMessageQueue.scala
// for not using it above
private[helper] class IElement[T] protected[helper](var value : Option[T], var count : Int) extends Ordered[IElement[T]]{
  var nextIsAccessible = new AtomicBoolean(false)
  var next : Option[IElement[T]] = None

  def compare(that: IElement[T]) =
    that.count - this.count

  def this( value : T, count : Int ) =
    this(Some(value), count)

  def setValue( newValue : T, c : Int ) {
    count = c
    value = Some(newValue)
  }

  def setNext ( newNext  : IElement[T] ) {
    next = Some(newNext)
    nextIsAccessible.set(true)
  }
}