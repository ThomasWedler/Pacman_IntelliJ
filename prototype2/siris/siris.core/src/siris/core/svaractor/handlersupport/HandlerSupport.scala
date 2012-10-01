package siris.core.svaractor.handlersupport

import scala.util.continuations.cps

object Types{
  private type hBase[T]    = PartialFunction[Any, T]
  type handler_t           = hBase[Unit]
  type handlerC_t          = hBase[Any]
  type cpsHandler_t        = hBase[Any @HandlerContinuation]
  type HandlerOption       = Option[(handlerC_t, ClassManifest[_])]
  type HandlerContinuation = cps[HandlerOption]
  
}

/* author: dwiebusch
 * date: 19.09.2010
 */

trait HandlerSupport{
  type handler_t           = Types.handler_t
  type handlerC_t          = Types.handlerC_t
  type cpsHandler_t        = Types.cpsHandler_t
  type handlerList_t       = List[(Handler, handler_t, Boolean)]
  type HandlerOption       = Types.HandlerOption
  type HandlerContinuation = Types.HandlerContinuation


  // referencing for handlers
  /**
   * This trait must be extended by data structes that identifies a registered
   * handler. The identification is used to remove a handler.
   *
   * So the operator ==, equals and hashCode must be implemented in the correct
   * way.
   */
  trait Handler

  protected type IdType          = Long
  protected type handlerType[T]  = Function[T, Unit]
  protected type handlerTypeC[T] = Function[T, Any @HandlerContinuation]

  // returns the reference of the registered handler
  /**
   * This method adds a handler that processes a message. The handling is
   * done by PartialFunctions that processes messages with the given data
   * type. After the function is registered a Handler is returned that
   * identifies the handler and can be used to remove it again.
   *
   * This method must not be called from outside of the actor. If it is called
   * from outside a SVarActorException is thrown.
   *
   *
   * @param handler A PartialFunction that processes messages of the given datatype
   * @return A handler to the registred handler.
   */


  def addHandler[T](handler: handlerType[T])(implicit manifest: ClassManifest[T])
  def addHandlerPF[T]( pf: PartialFunction[T, Unit] )( implicit manifest : ClassManifest[T] )
  def addHandlerC[T](handlerC: handlerTypeC[T])(implicit manifest: ClassManifest[T])
  def addSingleUseHandler[T]( f: Function[T, Unit] )( implicit manifest : ClassManifest[T] )
  def addSingleUseHandlerPF[T]( pf: PartialFunction[T, Any] )( implicit manifest : ClassManifest[T] )

  // removes a handler using the reference given by addHandler
  /**
   * This method removes a handler from am actor. The Handler Object
   * that was created by a previous call of addHandler must be used
   * to identify the handler.
   *
   * This method must not be called from outside of the actor. If it is called
   * from outside a SVarActorException is thrown.
   *
   * @param The handler that should be removed.
   */
  def removeHandler(id : Long, manifest : ClassManifest[_])

  protected def applyHandlers( msg : Any )
}