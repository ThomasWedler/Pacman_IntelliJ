package siris.core.entity.typeconversion

/* author: dwiebusch
 * date: 27.08.2010
 */

/**
 *  base class for unified handling of Provides and Requires
 */
abstract class ProvideAndRequire {
  def manifest: ClassManifest[_]
}

//O : OntologyType
//T : ComponentType

/**
 *  a class that wraps a ProvideConversionInfo. O should always be the type used in the Simulator Core
 */
case class Provide[T, O]( wrapped : ProvideConversionInfo[T, O] )(implicit val manifest: ClassManifest[Provide[T, O]]) extends ProvideAndRequire{
  //! loop the using method through
  def using( conv : IConverter[T, O] ) = Provide[T, O](wrapped.using(conv))

  /**
   *  sets the initial value of the svar to be created
   * @param value the initial value
   * @return the same Provide having the initial value variable set 
   */
  //TODO: one might want to return a copy instead of the same object (for reusability reasons)
  def withInitialValue( value : T ) : Provide[T,O] = {
    wrapped.setInitialValue( value )
    this
  }
}

/**
 *  a class that wraps a RequireConversionInfo. O should always be the type used in the Simulator Core
 */
case class Require[O, T]( wrapped : RequireConversionInfo[O, T] )(implicit val manifest: ClassManifest[Require[O, T]]) extends ProvideAndRequire{
  //! loop the using method through
  def using( conv : IReverter[T, O] ) = Require[O, T](wrapped.using(conv))
}

case class Own[O]( wrapped : ConvertibleTrait[O])(implicit val manifest : ClassManifest[ConvertibleTrait[O]]) extends ProvideAndRequire