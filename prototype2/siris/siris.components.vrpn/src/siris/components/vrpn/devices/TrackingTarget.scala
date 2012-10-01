package siris.components.vrpn.devices

import siris.core.entity.Entity
import siris.core.entity.component.Removability
import siris.components.naming.NameIt
import siris.components.vrpn.VRPN
import siris.ontology.{EntityDescription, Symbols}

/**
 * @author dwiebusch
 * Date: 05.07.11
 * Time: 11:03
 */

case class TrackingTarget(val url : String, val id : Symbol) extends VRPNAspect(Symbols.trackingTarget){
  protected def getCreateParams = addCVars{ Set(VRPN.id.apply(id), VRPN.url.apply(url)) }
  def getFeatures               = Set(VRPN.oriAndPos, VRPN.url, VRPN.id)
  def getProvidings             = getFeatures
}

case class SimpleTarget(val url : String, val id : Symbol ) {
  def realize( handler : (Entity with Removability) => Unit ) {
    desc.realize( handler )
  }

  private val desc = EntityDescription(
    TrackingTarget(url, id),
    NameIt("SimpleTarget" + url + "->" + id.name)
  )
}