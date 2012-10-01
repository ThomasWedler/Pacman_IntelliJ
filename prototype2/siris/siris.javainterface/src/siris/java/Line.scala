package siris.java

import siris.components.renderer.createparameter.ShapeFromFile
import simplex3d.math.floatm.FloatMath.{normalize, length, cross}
import siris.components.naming.NameIt
import siris.core.entity.Entity
import simplex3d.math.floatm.{ConstMat4f, Mat4x4f, Vec3f, Mat3x4f}
import siris.components.physics.ImplicitEitherConversion._
import siris.core.svaractor.{SVarActorLW, SVar}
import siris.core.entity.component.Removability
import siris.ontology.{EntityDescription, types => gt}


//Global Types
/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 8/19/11
 * Time: 4:36 PM
 */

object Line {

  /**
   * A description to preload the graphical aspect of lines.
   */
  def gfxPreloadDesc =
    EntityDescription(
      ShapeFromFile(
        file = "fishtank/data/models/red-1x0.05x0.05-positiveZAxis-cylinder.dae",
        scale = ConstMat4f(Mat3x4f.scale(1f)),
        transformation = ConstMat4f(Mat3x4f.translate(Vec3f(1000f, 1000f, 1000f)))
      ),
      NameIt("PreLoadLine")
    )
}

/**
 * A line between two transformation SVars.
 */
class Line(fromSVar : SVar[gt.Transformation.dataType], toSVar: SVar[gt.Transformation.dataType], red: Boolean = true) {

  /**
   * Tells the line actor to remive the line entity.
   */
  private case class RemoveLine()

  /**
   * Tells the line actor to initialize.
   */
  private case class Init()

  /**
   * The line actor which updates the line's transformation.
   */
  private val lineActor = new LineActor().start()

  /**
   * Removes the line and shuts down the line actor.
   */
  def remove() {
    lineActor ! RemoveLine()
  }

  /**
   * Updates a line's transformation.
   */
  private class LineActor() extends SVarActorLW  {

    private var lineEntity: Option[Entity with Removability] = None
    private var transformation: Option[SVar[Mat4x4f]] = None
    private var fromOption: Option[Vec3f] = None
    private var toOption: Option[Vec3f] = None
    private var removeEntityAfterCreation = false
    private val colladaFile = if(red) "fishtank/data/models/red-1x0.05x0.05-positiveZAxis-cylinder.dae"
                              else "fishtank/data/models/blue-1x0.05x0.05-positiveZAxis-cylinder.dae"

    private def update() {
      if (fromOption.isDefined && toOption.isDefined) {
        transformation.collect {
          case svar => svar.set(calculateLineTransformation(fromOption.get, toOption.get))
        }
      }
    }

    private def calculateLineTransformation(from: Vec3f, to: Vec3f): Mat4x4f = {
      val d = length(from - to)
      if (d > 0.001f) {
        val z = normalize(to - from)
        var y = if (math.abs(z.x) < 0.001f) Vec3f(z.x, z.z, z.y) else Vec3f(z.y, z.x, z.z)
        val x = normalize(cross(y, z))
        y = cross(z, x)
        Mat4x4f(Mat3x4f(x, y, z * d, from))
      } else
        Mat4x4f(Mat3x4f.scale(0.00001f).translate(from))
    }

    private def tryCreate() {
      if (fromOption.isDefined && toOption.isDefined) {

        EntityDescription(
          ShapeFromFile(
            file = colladaFile,
            scale = ConstMat4f(Mat3x4f.scale(Vec3f(1f / 16f, 1f / 16f, 1f))),
            transformation = ConstMat4f(calculateLineTransformation(fromOption.get, toOption.get))
          ),
          NameIt("Line")
        ).realize(e => {
          lineEntity = Some(e)
          transformation = Some(e.get(siris.ontology.types.Transformation).get)
          if (removeEntityAfterCreation) cleanUp()
        })
      }
    }

    def cleanUp() {
      fromSVar.ignore()
      toSVar.ignore()
      lineEntity.get.remove()
      shutdown()

    }

    override def startUp() {
      this ! Init()
    }

    addHandler[Init] {
      case msg =>
        fromSVar.observe{t: Mat4x4f => {fromOption = Some(t(3).xyz); update()}}
        fromSVar.get{t: Mat4x4f => {fromOption = Some(t(3).xyz); tryCreate()}}
        toSVar.observe{t: Mat4x4f => {toOption = Some(t(3).xyz); update()}}
        toSVar.get{t: Mat4x4f => {toOption = Some(t(3).xyz); tryCreate()}}
    }

    addHandler[RemoveLine]{case msg =>
      lineEntity match {
        case Some(e) => cleanUp()
        case None => removeEntityAfterCreation = true
      }
    }

  }

}

