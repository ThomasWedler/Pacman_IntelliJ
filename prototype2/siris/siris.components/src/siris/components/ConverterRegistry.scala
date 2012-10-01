package siris.components

import siris.ontology.SVarDescription
import siris.ontology.types.Transformation
import siris.ontology.referencesystems.CoordinateSystemConverter
import siris.core.entity.typeconversion.{ConvertibleTrait, Converter}

/**
 * User: dwiebusch
 * Date: 18.05.11
 * Time: 13:45
 */

object ConverterRegistry{
  def createCoordConverter[L]( localType      : SVarDescription[L, _],
                               revert         : Transformation.dataType => L,
                               convert        : L => Transformation.dataType,
                               coordConverter : CoordinateSystemConverter[Transformation.dataType] ) =
  {
    createConverter(localType, Transformation,
      (x : Transformation.dataType) => revert(coordConverter.fromRefCoords(x)),
      (x : L)                       => coordConverter.toRefCoords(convert(x)))
  }


  def createConverter[T, O](local : SVarDescription[T, _], ontology : SVarDescription[O, _], rev : O => T, conv : T => O) =
    new Converter(local, ontology){
      override def canConvert(from: ConvertibleTrait[_], to: ConvertibleTrait[_]) =
        from.typeinfo == local.typeinfo && to.typeinfo == ontology.typeinfo
      override def canRevert(to: ConvertibleTrait[_], from: ConvertibleTrait[_]) =
        to.typeinfo == local.typeinfo && from.typeinfo == ontology.typeinfo
      def revert(i: O) = rev(i)
      def convert(i: T) = conv(i)
    }
}