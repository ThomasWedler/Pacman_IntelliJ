package siris.applications.basicexample.objects

import siris.core.entity.Entity
import siris.core.entity.component.Removability
import siris.components.renderer.createparameter.{ReadFromElseWhere, ShapeFromFile}
import siris.components.physics.ImplicitEitherConversion._
import siris.components.physics.PhysSphere
import siris.components.naming.NameIt
import simplex3d.math.floatm.{ConstVec3f, Mat3x4f, ConstMat4f}
import siris.ontology.EntityDescription

/**
 * User: dwiebusch
 * Date: 26.05.11
 * Time: 13:06
 */

case class Ball(val name : String, val radius : Float, val position : ConstVec3f){
  def realize(handler : (Entity with Removability) => Any) { desc.realize(handler) }

  val desc = EntityDescription(
    PhysSphere(
      restitution    = 0.99f,
//      transform      = position,
      radius         = radius
    ),
    ShapeFromFile(
      file           = "annualfair/vis/ball.dae",
      scale          = ConstMat4f(Mat3x4f.scale(radius*2f)),
      transformation = ConstMat4f(Mat3x4f.translate(position)) // ReadFromElseWhere
    ),
    NameIt(name)
  )
}