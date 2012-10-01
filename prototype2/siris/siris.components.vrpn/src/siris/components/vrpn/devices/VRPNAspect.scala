package siris.components.vrpn.devices

import siris.ontology.Symbols
import siris.core.entity.description.{SVal, Aspect, Semantics}

/**
 * @author dwiebusch
 * Date: 05.07.11
 * Time: 11:02
 */

abstract class VRPNAspect(sem : SVal[Semantics], targets : List[Symbol] = Nil) extends Aspect(Symbols.vrpn, sem, targets)