package siris.ontology

import siris.core.entity.description.{AspectBase, IEntityDescription}
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.entity.component.Removability
import siris.core.entity.Entity

/**
 * @author dwiebusch
 * Date: 06.09.11
 * Time: 16:22
 */

final case class EntityDescription(aspects : AspectBase*)
  extends SpecificDescription(PlainEntity, aspects.toList)

protected abstract class SpecificDescription[T <: Entity]( entityDesc : EntitySVarDescription[T],
                                                           aspects    : List[AspectBase],
                                                           features   : ConvertibleTrait[_]* )
  extends IEntityDescription[T](entityDesc, entityDesc.ctor, None,
    if (features.nonEmpty) FeatureDefinition(features.toSet) :: aspects else aspects)

private object PlainEntity
  extends EntitySVarDescription(Symbols.entity, new siris.core.entity.Entity(_) with Removability)

private case class FeatureDefinition(getFeatures : Set[ConvertibleTrait[_]]) extends AspectBase {
  def toEntityAspect  = throw FeatureDefinitionException()
  def getProvidings   = Set()
}

case class FeatureDefinitionException()
  extends Exception("FeatureDefinition instances are only meant to define features, don't use them for entity creation")
