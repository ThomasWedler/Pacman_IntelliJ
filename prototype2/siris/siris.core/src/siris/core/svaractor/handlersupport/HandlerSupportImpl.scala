package siris.core.svaractor.handlersupport

import scala.util.continuations._
import scala.collection.mutable
import scala.reflect.ClassManifest._

/* author: dwiebusch
 * date: 19.09.2010
 */

trait HandlerSupportImpl extends HandlerSupport{
  private type IdHandlerPair   = (IdType, handlerType[Any])
  private type handlerList     = List[IdHandlerPair]

  private var lastId : IdType  = 0
  private val handlers         = mutable.Map[ClassManifest[_], handlerList]()

  protected val neverMatchingHandler = new handler_t {
    def isDefinedAt(x: Any) = false
    def apply(v1: Any) {}
  }

  protected val handlersAsPF = new PartialFunction[Any, Unit] {
    def apply(x: Any) { applyHandlers(x) }
    def isDefinedAt(x: Any) = true
  }

  def addHandlerC[T](pf: handlerTypeC[T])(implicit manifest: ClassManifest[T]) {
    addHandler( wrapHandler(pf) )( manifest )
  }

  def addHandler[T](handler: Function[T, Unit])(implicit manifest: ClassManifest[T]) {
    handler match{
      case pf : PartialFunction[_, _] => updateHandlers( manifest, (generateId(), handler.asInstanceOf[PartialFunction[Any, Unit]] ) )
      case f => updateHandlers( manifest, (generateId(), (msg : Any) => handler(msg.asInstanceOf[T]) ) )
    }
  }

  def addHandlerPF[T]( pf: PartialFunction[T, Unit] )( implicit manifest : ClassManifest[T] ) {
    addHandler[T](pf)(manifest)
  }

  def removeHandler( id: Long, manifest : ClassManifest[_]) {
    handlers.update(manifest, handlers.getOrElse(manifest, Nil).filterNot( _._1 == id ) )
  }

  def addSingleUseHandler[T]( f: handlerType[T] )( implicit manifest : ClassManifest[T] ) {
    addSingleUseHandler(wrap(f), manifest)
  }

  def addSingleUseHandlerPF[T]( pf: PartialFunction[T, Any] )( implicit manifest : ClassManifest[T] ) {
    addSingleUseHandler(pf.asInstanceOf[PartialFunction[Any, Any]], manifest)
  }

  protected def getManifest[T]( value : T ) : ClassManifest[T] =
    fromClass(value.asInstanceOf[AnyRef].getClass).asInstanceOf[ClassManifest[T]]

  protected def applyHandlers(msg : Any) {
    val msgManifest = getManifest( msg )
    handlers get msgManifest match{
      case Some(list) => applyHandlerList( list, msg )
      case None => for ( ( manifest, list ) <- handlers )
        if (manifest >:> msgManifest){
          applyHandlerList( list, msg )
          return
        }
    }
  }

  private def storeContinuation( hOpt : HandlerOption ) {
    hOpt collect { case (handler, manifest) => addSingleUseHandler(handler, manifest) }
  }

  private def addSingleUseHandler[T](pf: handlerC_t, manifest : ClassManifest[_]) {
    updateHandlers(manifest, toRemovableTuple(pf, manifest, generateId() ), true )
  }

  private def toRemovableTuple( pf : handlerC_t, manifest : ClassManifest[_], id : IdType ) =
    id -> pf.andThen{ _ => removeHandler(id, manifest) }

  private def generateId() : IdType =
    (lastId +=1, lastId)._2

  private def updateHandlers( manifest : ClassManifest[_], toAdd : IdHandlerPair, append : Boolean = false ) {
    handlers.update(manifest, if (append) handlers.getOrElse(manifest, Nil) :+ toAdd else toAdd :: handlers.getOrElse(manifest, Nil) )
  }

  private[svaractor] def continueWith[A, T]( f : Function[A, T])(implicit manifest : ClassManifest[A]) : T @HandlerContinuation =
    shift { (fun : T => HandlerOption) => Some( wrap(f) andThen fun andThen storeContinuation, manifest ) }

  private def applyHandlerList( list : handlerList, msg : Any ) {
    list.headOption.collect {
      case tuple => tuple._2 match {
        case pf : PartialFunction[_, _] => if( pf.isDefinedAt(msg) ) pf( msg ) else applyHandlerList(list.tail, msg)
        case f => f(msg)
      }
    }
  }

  private def wrapHandler[T](f : handlerTypeC[T])(implicit manifest : ClassManifest[T]) = new PartialFunction[T, Unit] {
    private val isPf = f.isInstanceOf[PartialFunction[_, _]]
    def apply( msg : T ) { storeContinuation( reset{ f( msg ); None } ) }
    def isDefinedAt( msg : T ) = if (isPf) f.asInstanceOf[PartialFunction[T, _]].isDefinedAt( msg ) else true
  }

  private def wrap[T, U](f : Function[T, U])(implicit manifest : ClassManifest[T]) : PartialFunction[Any, U] = new PartialFunction[Any, U]{
    def isDefinedAt( x : Any ) =
      if (fromClass(getManifest(x).erasure) <:< fromClass(manifest.erasure)) definedAt( x.asInstanceOf[T] ) else false
    def apply(v1: Any) = f.apply(v1.asInstanceOf[T])
    private val definedAt : T => Boolean = f match {
      case pf : PartialFunction[_, _] => pf.asInstanceOf[PartialFunction[T, U]].isDefinedAt _
      case _ => ( x : Any ) => true
    }
  }
}
