package siris.components.renderer.jvr.types

/**
 * Created by IntelliJ IDEA.
 * User: dwiebusch
 * Date: 28.03.12
 * Time: 11:57
 * To change this template use File | Settings | File Templates.
 */

import javax.media.opengl.GL
import simplex3d.math.floatm.Vec3f
import de.bht.jvr.core.attributes.{AttributeVector2, AttributeVector3, AttributeVector4}
import de.bht.jvr.math.{Vector2, Vector3, Vector4}
import de.bht.jvr.core._


object Face{
  def apply(x : Int, y : Int, z : Int) : Face = Face((x, y, z))
}

case class Data( points : Array[Vec3f], normals : Array[Vec3f], vertices : Array[Vertex], faces : List[Face],
                 colors : Option[Array[Vec3f]] = None, texCoords : Option[Array[Float]] = None)
case class Vertex(vertexIdx : Int, normalIdx : Int, texIdx : Int)
case class Face(indices : (Int, Int, Int)) {
  def foreach[T](handler : Int => T) =
    List(handler(indices._1), handler(indices._2), handler(indices._3))
}

class MeshGeometry(private var data : Data) extends Geometry {
  private var attributeCloud = new AttributeCloud(data.faces.length, GL.GL_TRIANGLES)

  def isBuilt(ctx: Context) =
    attributeCloud.isBuilt(ctx)

  def getBBox =
    attributeCloud.getBBox

  def getRenderClone =
    attributeCloud.getRenderClone

  def pick(ray: PickRay) =
    attributeCloud.pick(ray)

  def render(ctx: Context) {
    attributeCloud.render(ctx)
  }

  def setData(newData : Data, newFaces : List[Face]) {
    synchronized{
      data  = newData
    }
    updateGeometry()
  }

  private def updateGeometry() {
    val jvrTexCoord = new java.util.ArrayList[Vector2]()
    val jvrVertices = new java.util.ArrayList[Vector4]()
    val jvrNormals  = new java.util.ArrayList[Vector3]()

    def idx2vertex(idx : Int) = {
      val tmp = data.points(idx)
      new Vector4(tmp.x, tmp.y, tmp.z, 1)
    }

    def idx2normal(idx : Int) = {
      val tmp = data.normals(idx)
      new Vector3(tmp.x, tmp.y, tmp.z)
    }

    for (face <- data.faces){
      face.foreach{ vertex =>
        jvrTexCoord.add(new Vector2(0, 0))
        jvrNormals.add(idx2normal(data.vertices(vertex).normalIdx))
        jvrVertices.add(idx2vertex(data.vertices(vertex).vertexIdx))
      }
    }

    attributeCloud = new AttributeCloud(data.faces.length * 3, GL.GL_TRIANGLES)
    attributeCloud.setAttribute("jvr_Vertex", new AttributeVector4(jvrVertices))
    attributeCloud.setAttribute("jvr_Normal", new AttributeVector3(jvrNormals))
    attributeCloud.setAttribute("jvr_TexCoord", new AttributeVector2(jvrTexCoord))
  }

  updateGeometry()
}


