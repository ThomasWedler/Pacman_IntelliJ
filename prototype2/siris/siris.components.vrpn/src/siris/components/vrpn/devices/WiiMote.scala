package siris.components.vrpn.devices

import siris.components.naming.NameIt
import siris.core.entity.description._
import siris.core.entity.component.Removability
import siris.core.entity.Entity
import siris.ontology.types._
import siris.ontology.{EntityDescription, Symbols}

/* author: dwiebusch
 * date: 25.09.2010
 */

case class WiiMote(val wiiUrl : String) {
  def realize(handler : (Entity with Removability) => Any) {
    EntityDescription(
      VRPNButton(Key_b,     wiiUrl, Symbol("2")),
      VRPNButton(Key_Left,  wiiUrl, Symbol("8")),
      VRPNButton(Key_Right, wiiUrl, Symbol("9")),
      VRPNButton(Key_Down,  wiiUrl, Symbol("10")),
      VRPNButton(Key_Up,    wiiUrl, Symbol("11")),
      NameIt(Symbols.wiiMote.toString + "@" + wiiUrl)
    ).realize(handler)
  }
}

case class WiiMoteWithTracker(val wiiUrl : String, val trackerUrl : String, val targetId : Symbol) {
  def realize(handler : (Entity with Removability) => Any) {
    EntityDescription(
      TrackingTarget(trackerUrl, targetId),
      VRPNButton(Key_b,     wiiUrl, Symbol("2")),
      VRPNButton(Key_Left,  wiiUrl, Symbol("8")),
      VRPNButton(Key_Right, wiiUrl, Symbol("9")),
      VRPNButton(Key_Down,  wiiUrl, Symbol("10")),
      VRPNButton(Key_Up,    wiiUrl, Symbol("11")),
      NameIt(Symbols.wiiMote.toString + " " + wiiUrl + " && " + trackerUrl)
    ).realize(handler)
  }
}