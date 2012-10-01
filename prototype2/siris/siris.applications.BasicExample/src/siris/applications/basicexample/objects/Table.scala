package siris.applications.basicexample.objects

import siris.core.entity.Entity
import siris.core.entity.component.Removability
import siris.components.renderer.createparameter.{ReadFromElseWhere, ShapeFromFile}
import siris.components.physics.ImplicitEitherConversion._
import siris.components.physics.PhysBox
import siris.components.naming.NameIt
import simplex3d.math.floatm.{Vec3f, ConstMat4f, Mat3x4f, ConstVec3f}
import siris.ontology.EntityDescription

/**
 * User: dwiebusch
 * Date: 26.05.11
 * Time: 13:25
 */

case class Table(val name : String, val size : Vec3f, val position : ConstVec3f){
  def realize(handler : (Entity with Removability) => Any) { desc.realize(handler) }

  val desc = EntityDescription(
    PhysBox(
      halfExtends    = size*0.5f,
  //   transform      = position,
      restitution    = 0.99f,
      mass           = 0f
    ),
    ShapeFromFile(
      transformation = ConstMat4f(Mat3x4f.translate(position)),   //ReadFromElseWhere,
      scale          = ConstMat4f(Mat3x4f.translate(ConstVec3f(0,1,0)).scale(ConstVec3f(size.x/3f, size.y/2f, size.z))),
      file           = "annualfair/vis/table.dae"
    ),
    NameIt(name)
  )
}
