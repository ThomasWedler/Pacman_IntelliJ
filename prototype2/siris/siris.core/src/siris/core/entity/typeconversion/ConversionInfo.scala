package siris.core.entity.typeconversion

import siris.core.entity.Entity
import siris.core.svaractor.{SVarImpl, SVar}
import java.lang.Exception
import siris.core.entity.description.{SVal, Semantics}

/* author: dwiebusch
* date: 27.08.2010
*/

/**
 *  base class for conversion infos
 */
trait ConversionInfo[T, O] {
  //! the input type
  def from : ConvertibleTrait[T]
  //! the output type
  def to   : ConvertibleTrait[O]
  //! the converter to be used, will be set automagically
  private var theConverter : Option[IConverter[T, O]] = None
  //! the reverter to be used, will be set automagically
  private var theReverter  : Option[IReverter[T, O]]  = None

  /**
   * sets the con- and reverter, requiring an IConverter
   * @param c a converter which will be casted into a reverter to fill necessary variables
   * @throws NoConverterFoundException if converter did not match converting requirements
   * @throws NoReverterFoundException if converter did not match reverting requirements
   */
  protected def setVerters(c : IConverter[T, O]) {
    theConverter = if (c._canConvert(from, to)) Some(c) else throw new NoConverterFoundException(from, to)
    theReverter  = c match {
      case r : IReverter[_, _] if (r._canRevert(from, to)) => Some(r.asInstanceOf[IReverter[T, O]])
      case _ => throw new NoReverterFoundException(from, to)
    }
  }

  /**
   * sets the con- and reverter, requiring an IReverter
   * @param c a converter which will be casted into a reverter to fill necessary variables
   * @throws NoConverterFoundException if converter did not match converting requirements
   * @throws NoReverterFoundException if converter did not match reverting requirements
   */
  protected def setVerters(r : IReverter[T, O]) {
    theReverter = if (r._canRevert(from, to)) Some(r) else throw new NoReverterFoundException(from, to)
    theConverter  = r match {
      case c : IConverter[_, _] if (c._canConvert(from, to)) => Some(c.asInstanceOf[IConverter[T, O]])
      case _ => throw new NoConverterFoundException(from, to)
    }
  }

  /**
   * returns the name of the svars created by the conversion info which is used to inject the svar into an entity
   * @return the name of the svars created by the conversion info which is used to inject the svar into an entity  
   */
  def getSVarName =
    from.sVarIdentifier

  /**
   *  returns the reverter used by this conversioninfo. If it was not set before, a matching registered reverter
   * is set and returned 
   * @return the reverter used by this conversioninfo.
   */
  def accessReverter() : IReverter[T, O] = if (theReverter.isDefined) theReverter.get else {
    theReverter = Some(Reverter(from, to))
    theReverter.get
  }

  /**
   *  returns the converter used by this conversioninfo. If it was not set before, a matching registered converter
   * is set and returned
   * @return the converter used by this conversioninfo.
   */
  def accessConverter() : IConverter[T, O] = if (theConverter.isDefined) theConverter.get else {
    theConverter = Some(Converter(from, to))
    theConverter.get
  }
}

/**
 *  Representation of a conversion used to provide svars. Reads like provide a svar of type T as a svar of type O.
 * Actually creates a svar of type O, injects it to an entity and returns an adapter which looks like a svar of type T 
 */
class ProvideConversionInfo[T, O](val from : ConvertibleTrait[T], val to : ConvertibleTrait[O],
                                  private var annotations : Set[SVal[Semantics]]= Set()) extends ConversionInfo[T, O] {
  //! the optional initial value
  private var initialValue : Option[T] = None

  /**
   *  sets the initial value
   *
   * @param value the initial value
   */
  def setInitialValue( value : T ) {
    initialValue = Some(value)
  }

  def addAnnotations( as : Set[SVal[Semantics]] ) {
    annotations = annotations ++ as
  }

  /**
   *  defines the converter to be used
   * @param c the converter to be used
   * @return the conversion info having the converter set
   */
  //TODO: perhaps a copy should be returned to ensure reusability
  def using(c: IConverter[T, O]) : ProvideConversionInfo[T, O] = {
    setVerters(c)
    this
  }

  /**
   *  creates and injects a svar into an entity. Uses the initial value if set, calls the constructor provided
   * with the from-convertible otherwise
   * @param the entity into which the created svar should be injected
   * @return a tuple of a wrapped svar having the required type and the entity after injecting the svar
   */
  protected[entity] def injectSVar( e : Entity ) : ( SVar[T], Entity ) = {
    val svar   = SVarImpl(accessConverter().convert( initialValue.getOrElse( from.defaultValue() ) ) )
    val entity = e.injectSVar(getSVarName, svar, to, annotations)
    new ConvertedSVar(svar, this) -> entity
  }
}

// the conversion is kind of inverted, as the adapter has to do the same stuff as the provided version does
// in other words: even though the adapter looks like some output converter, it is an input converter
class RequireConversionInfo[O, T](val to : ConvertibleTrait[O], val from : ConvertibleTrait[T])
  extends ConversionInfo[T, O]{
  /**
   *  defines the reverter to be used
   * @param c the reverter to be used
   * @return the conversion info having the reverter set
   */
  //TODO: perhaps a copy should be returned to ensure reusability
  def using(c: IReverter[T, O]) : RequireConversionInfo[O, T] = {
    setVerters(c)
    this
  }

  /**
   *  retrieves the svar specified by this conversion info, wraps it and returns the wrapper (which mimics a svar
   * of the required type)
   * @param the entity from which the svar should be looked up
   * @return a svar of the required type
   */
  def createAdapter( e : Entity ) : SVar[T] =
    e.get(from, accessReverter()).getOrElse(throw new Exception("SVar not found: " + from.sVarIdentifier.name))
}
