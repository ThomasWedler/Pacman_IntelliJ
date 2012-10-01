package siris.core.entity.typeconversion

import reflect.ClassManifest
import siris.core.entity.description.{Semantics, SVal}
import java.lang.Exception

/* author: dwiebusch
 * date: 27.08.2010
 */

/**
 *  the convertible trait, which provides the information needed to create Provides and Requires
 */
trait ConvertibleTrait[T1]{
  //! The represented type.
  type dataType      = T1
  //! the class manifest of the represented type
  def typeinfo       : ClassManifest[T1]
  //! the semantics of the represented type
  def semantics      : Semantics
  //!
  def annotations    : Set[SVal[Semantics]]
  //! the base of this convertible trait
  protected val base : ConvertibleTrait[_]
  //! the identifier which is used to inject and lookup svars into/from entities
  val sVarIdentifier : Symbol = semantics.toSymbol

  def getBase =
    base.asInstanceOf[ConvertibleTrait[base.dataType]]

  /**
   * creates an instance of the represented type
   * @return an instance of the represented type
   */
  def defaultValue() : T1

  /**
   * returns a Provide that creates a non-conversion-ConversionInfo
   * @return a Provide that creates a non-conversion-ConversionInfo
   */
  def isProvided : Provide[T1, _] = providedAs(this)

  /**
   * returns a Require that creates a non-conversion-ConversionInfo
   * @return a Require that creates a non-conversion-ConversionInfo
   */
  def isRequired : Require[_, T1] = requiredAs(this)

  /**
   * returns an Own containing this ConvertibleTrait
   * @return an Own containing this ConvertibleTrait
   */
  def isOwned : Own[T1] = Own(this)

  /**
   * creates a Provide that will create a svar of type T1 and inject a svar of type T2
   * @param o information on the type of the svar to be injected
   * @return a Provide that will create a svar of type T1 and inject a svar of type T2
   */
  final def providedAs[T2](o: ConvertibleTrait[T2]) = Provide(new ProvideConversionInfo[T1, T2](this, o, annotations))

  /**
   * creates a Require that will look up a svar of type T1 and return it as a svar of type T2
   * @param o information on the type of the svar to be returned when the lookup has finished
   * @return a Require that will look up a svar of type T1 and return it as a svar of type T2
   */
  final def requiredAs[T2](o: ConvertibleTrait[T2]) = Require(new RequireConversionInfo[T1, T2](this, o))

  /**
   *
   * Augments the ConvertibleTrait with a value to create a complete CreateParam.
   */
  def apply(value: T1) : SVal[T1] =
    SVal(this)(value)

  override def toString =
    sVarIdentifier.name

  override def equals(p1: Any) = p1 match {
    case p : ConvertibleTrait[_] =>
      p.sVarIdentifier == sVarIdentifier &&
        p.annotations.diff(annotations).isEmpty &&
        annotations.diff(p.annotations).isEmpty
    case _ => false
  }

  override def hashCode() =
    41 * ( 41 + sVarIdentifier.hashCode() ) + annotations.hashCode()
}

case class NoBaseDefinedException( from : ConvertibleTrait[_], to : ConvertibleTrait[_] )
  extends Exception("Cannot convert to " + to + " because base is defined for ConvertibleTrait " + from)