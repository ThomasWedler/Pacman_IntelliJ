package siris.core.helper

/* author: dwiebusch
 * date: 02.10.2010
 */


private[helper] class MemPool[T]{
  private final val root  = new IElement[T](None, 0)
  private var tail = root

  def returnToPool( element : IElement[T] ) {
    //reset element
    element.nextIsAccessible.set(false)
    element.next = None
    //add element
    tail.setNext(element)
    //update tail
    tail = element
  }

  /**
   * we do ensure that we don't take all elements, to prevent concurrent access to the tail element
   */
  def take( initialValue : T, id : Int ) : IElement[T] =
    if ( root.nextIsAccessible.get && root.next.exists( _ != tail) ){
      val retVal = root.next.get
      root.next = retVal.next
      root.nextIsAccessible.set( retVal.nextIsAccessible.get )
      retVal.setValue( initialValue, id )
      retVal
    }
    else new IElement( initialValue, id )
}
