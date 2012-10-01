package siris.core.entity.typeconversion

import siris.core.svaractor.{SVarActor, SVar}
import actors.Actor

/* author: dwiebusch
 * date: 27.08.2010
 */

/**
 *  class that wraps a svar with an converter
 *
 * Note:
 * internalType = O
 * externalType = T
 */
protected[entity] class ConvertedSVar[O, T](wrappedSVar : SVar[O], c : ConversionInfo[T, O]) extends SVar[T]{
  /**
   * constructor for ease of use
   */
  protected[entity] def this( wrappedSVar : SVar[O], par : Require[O, T] ) = this(wrappedSVar, par.wrapped)
  //! the convert function
  private val convert : (T) => O = c.accessConverter().convert
  //! the revert function
  private val revert  : (O) => T = c.accessReverter().revert
  //! conversions, implicit for nicer code
  private implicit def converter( input : (T) => Unit ) = (x : O) => input( revert(x) )
  //! reversions, implicit for nicer code
  private implicit def converter( input : T ) : O = convert(input)
  //! loop method calls through to the wrapped svar
  def update( updateMethod : T => T )  { wrappedSVar.update( (v : O) => updateMethod(revert(v))) }
  //! loop method calls through to the wrapped svar
  def get                              = wrappedSVar.get.asInstanceOf[T]
  //! loop method calls through to the wrapped svar
  def observe(handler: (T) => Unit)    { wrappedSVar.observe(handler) }
  //! loop method calls through to the wrapped svar
  def owner(owner: SVarActor)          { wrappedSVar.owner(owner) }
  //! loop method calls through to the wrapped svar
  def get(consume : (T) => Unit)       { wrappedSVar.get(consume) }
  //! loop method calls through to the wrapped svar
  def set(value : T)                   { wrappedSVar.set(value) }
  //! loop method calls through to the wrapped svar
  def ignore()                         { wrappedSVar.ignore() }
  //! loop method calls through to the wrapped svar
  def owner()                          = wrappedSVar.owner()
  //! set the classmanifest of the contained value
  val containedValueManifest: ClassManifest[T]                    = c.from.typeinfo.asInstanceOf[ClassManifest[T]]
  //! loop method calls through to the wrapped svar
  def observe(ignoredWriters : Set[Actor])(handler: (T) => Unit)  { wrappedSVar.observe(ignoredWriters)(handler) }
  //! redicrect id lookups to wrapped svar
  final def id = wrappedSVar.id
}