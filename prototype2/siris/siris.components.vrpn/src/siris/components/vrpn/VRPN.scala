package siris.components.vrpn

import devices.SimpleTarget
import siris.ontology.{Symbols, types}
import siris.core.entity.Entity
import siris.core.entity.component.Removability
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.svaractor.{SVar, SVarActorHW}
import util.Random
import simplex3d.math.floatm.{Vec3f, Mat3x4f, Mat4x4f}

/* author: dwiebusch
* date: 23.09.2010
*/

object VRPN{
  val button      = types.Boolean as Symbols.button
  val position    = types.Transformation as Symbols.position
  val orientation = types.Transformation as Symbols.orientation
  val oriAndPos   = types.Transformation
  val text        = types.String
  val analog      = types.Real as Symbols.analogInput


  val url = types.String as Symbols.uRL
  val id = types.Identifier as Symbols.trackingTargetId
  val updateRateInMillis = types.Long as Symbols.refreshRate

  def main(args : Array[String]) {
    println("Hello VRPN!")
    new VRPNConnector().start()
    SimpleTarget("Tracker0@132.187.205.92", Symbol("0")).realize(handleEntity(_, VRPN.oriAndPos))
  }

  private def handleEntity( e : Entity with Removability, c : ConvertibleTrait[Mat4x4f] ) {
    new SVarActorHW{
      override def startUp() {
        e.execOnSVar(c){
          svar : SVar[Mat4x4f] =>
            svar.get(v => println("value is " + prettyPrintMat(v)))
            svar.observe(v => {
              println("value is " + prettyPrintMat(v))
              val tmp = Mat4x4f(Mat3x4f.translate(Vec3f(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())))
              println("setting value to " + prettyPrintMat(tmp))
              svar.set(tmp)
            })
        }
      }
    }.start()
  }

  def prettyPrintMat( mat : Any ) : String =
    "\n " + mat.toString.replaceAll(";", "\n").split("[\\(\\)]")(1)

}