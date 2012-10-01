package siris.applications.basicexample.objects

import siris.core.entity.Entity
import siris.core.entity.component.Removability
import siris.components.physics.ImplicitEitherConversion._
import siris.components.renderer.createparameter.SpotLight
import siris.components.naming.NameIt
import simplex3d.math.floatm.FloatMath.radians
import simplex3d.math.floatm.renamed._
import java.awt.Color
import siris.ontology.EntityDescription

/**
 * @author dwiebusch
 * Date: 01.07.11
 * Time: 11:18
 */

case class Light(val lightName : String, val pos : Vec3, val rotX : Float, val rotY : Float, val rotZ : Float) {
  def realize(f : (Entity with Removability) => Any) { desc.realize(f) }

  private val transform = Mat3x4.rotateZ(radians(rotZ)).rotateY(radians(rotY)).rotateX(radians(rotX)).translate(pos)
  val desc = EntityDescription(
    SpotLight(
      name           = lightName,
      transformation = ConstMat4(transform),
      diffuseColor   = new Color(0.5f, 0.6f, 0.5f),
      specularColor  = new Color(0.5f, 0.6f, 0.5f)
    ),
    NameIt(lightName)
  )
}