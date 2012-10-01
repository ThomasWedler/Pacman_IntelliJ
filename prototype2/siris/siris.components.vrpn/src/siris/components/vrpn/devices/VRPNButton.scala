package siris.components.vrpn.devices

import siris.ontology.Symbols
import siris.components.vrpn.VRPN
import siris.core.entity.typeconversion.ConvertibleTrait

/**
 * @author dwiebusch
 * Date: 05.07.11
 * Time: 12:04
 */

case class VRPNButton(val button : ConvertibleTrait[Boolean], val url : String, val id : Symbol,
                  override val targets : List[Symbol] = Nil) extends VRPNAspect(Symbols.button) {
  protected def getCreateParams = addCVars{ button(false) and VRPN.id.apply(id) and VRPN.url(url) }
  def getProvidings = getFeatures
  def getFeatures = Set(button)
}