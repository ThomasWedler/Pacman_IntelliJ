package siris.applications.basicexample.component

import siris.core.entity.Entity
import siris.core.entity.component.Removability
import siris.components.naming.NameIt
import siris.components.renderer.createparameter.ShapeFromFile
import simplex3d.math.floatm.{ConstMat4f, Mat3x4f, ConstVec3f}
import siris.ontology.EntityDescription

/**
 * @author dwiebusch
 * Date: 01.07.11
 * Time: 17:05
 */

case class ExampleBall(val name : String, val radius : Float, val position : ConstVec3f){
  def realize(handler : (Entity with Removability) => Any) { desc.realize(handler) }

  private val desc = EntityDescription(
    ExampleSphereAspect(
      id             = Symbol("myId")
    ),
    ShapeFromFile(
      file           = "annualfair/vis/ball.dae",
      scale          = ConstMat4f(Mat3x4f.scale(radius*0.5f)),
      transformation = Right(ConstMat4f(Mat3x4f.translate(position)))
    ),
    NameIt(name)
  )
}