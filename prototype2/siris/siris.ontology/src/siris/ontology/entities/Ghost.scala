package siris.ontology.entities

import siris.core.entity.Entity
import siris.core.entity.description.AspectBase
import siris.core.entity.component.Removability
import siris.ontology.{types, SpecificDescription, EntitySVarDescription, Symbols}

/**
 * @author dwiebusch
 * Date: 20.09.11
 * Time: 09:48
 */


case class GhostEntityDescription( aspects : AspectBase* )
  extends SpecificDescription(GhostDescription, aspects.toList, types.Transformation)

object GhostDescription
  extends EntitySVarDescription(Symbols.ghost, new Ghost(_) with Removability)

class Ghost( e : Entity = new Entity )
  extends Entity(e)