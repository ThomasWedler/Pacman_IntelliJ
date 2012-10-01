/*
  * Created by IntelliJ IDEA.
 * User: martin
 * Date: 4/7/11
 * Time: 12:57 PM
 */
package siris.components.vrpn

import xml.Node
import java.io.File
import devices.TrackingTarget
import simplex3d.math.floatm.FloatMath._
import simplex3d.math.floatm.{Vec3f, Mat3x4f, ConstMat4f}
import siris.components.naming.NameIt
import siris.core.component.{ComponentDoesNotExistException, Component}
import siris.core.entity.description._
import siris.core.svaractor.SVar
import siris.core.entity.Entity
import siris.ontology.{EntityDescription, types => gt}

/**
 *
 * Manages the data of the tracker described in descFile.
 * @see   ./SIRIS_ROOT/config/tracker/trackerDescription.xsd for more information on the description file.
 */
class TrackerDescription(descFile: File) {

  /**
   * Creates entities for all targets.
   */
  def createEntities() {
    try {
      if (!entitiesCreated) {
        Component('vrpnconnector)
        entitiesCreated = true
        for ((name, id) <- targets)
          EntityDescription(
            NameIt(name),
            TrackingTarget(url = url, id = Symbol(id))
          ).realize{ e => targetEntities += name -> e }
      }
    }
    catch {
      case e: ComponentDoesNotExistException =>
        println("TrackerDescription(" + descFile.getName + "): VRPN Connector has to be created before a call to createEntities.")
    }
  }

  /**
   *  Returns the entity corresponding to the targetName specified in the description file.
   *  A call to createEntities should already have taken place.
   */
  def getEntityFor(targetName: String) = targetEntities.get(targetName)

  /**
   *        Propagates the position and orientation of the target from to the svar to.
   *
   *     Thereby the data is transformed from the tracking coordinate system to the display
   *              coordinate system. This is achieved using the observe mechanism. The actual handlers
   *              are registered at the calling SVarActor.
   *
   * @param from  The name of the target from which position and orientation has to be propagated.
   *              This string referes to the names given in the description file.
   *
   * @param to    The svar to write the data into.
   */
  def propagateDisplayCoordinates(from: String, to: SVar[gt.Transformation.dataType]) {
    targetEntities.get(from).collect{case targetEntity: Entity =>
      val targetSvar = targetEntity.get(VRPN.oriAndPos.createdBy(simplex3d.math.floatm.renamed.Mat4x4(simplex3d.math.floatm.renamed.Mat4x4.Identity))).get
      addTransformationProperagtion(targetSvar, to, trackerToDisplay)
    }
  }

  private def addTransformationProperagtion(source: SVar[gt.Transformation.dataType], sink: SVar[gt.Transformation.dataType], sourceToSinkTransform: ConstMat4f) {
    source.observe((srcTrans) => {sink.set(inverse(sourceToSinkTransform) * srcTrans)})
  }

  private val doc = siris.core.helper.SchemaAwareXML.loadFile(descFile)

  //Currently unchecked since vrpn is the only choice
  //private val connection = (doc \ "connection").text

  private val url = (doc \ "url").text

  private val trackerToDisplay: ConstMat4f = readTransformFromXML((doc \ "trackerCoordinateSystemToDisplayCoordinateSystemTransformation").head)

  //Name -> entity
  private val targetEntities = collection.mutable.Map[String, Entity]()
  //Name -> Id
  private val targets = collection.mutable.Map[String, String]()

  (doc \\ "target").foreach((n: Node) => {
    val name = (n \ ("name")).text
    val id = (n \ ("id")).text
    targets += (name -> id)
  })

  private var entitiesCreated = false

  private def readTransformFromXML(n: Node): ConstMat4f = {
    val rotateX = (n \ ("rotateX")).text.toFloat
    val rotateY = (n \ ("rotateY")).text.toFloat
    val rotateZ = (n \ ("rotateZ")).text.toFloat

    val scale = (n \ ("scale")).text.toFloat

    val translateX = (n \ ("translateX")).text.toFloat
    val translateY = (n \ ("translateY")).text.toFloat
    val translateZ = (n \ ("translateZ")).text.toFloat

    ConstMat4f(
      Mat3x4f.rotateX(radians(rotateX)).rotateY(radians(rotateY)).rotateZ(radians(rotateZ)).scale(scale).translate(Vec3f(translateX, translateY, translateZ))
    )
  }
}