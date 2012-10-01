package siris.components.network

import siris.ontology.{types, Symbols}
import siris.core.entity.description.{SVal, Aspect, Semantics}

/**
 * Base class for all CreateParameterSets send to network components
 */
abstract class NetworkAspect(aspectType : SVal[Semantics], targets : List[Symbol] = Nil)
  extends Aspect(Symbols.network, aspectType, targets)
{
}

case class NetBaseProps (id : java.lang.Integer ) {

}