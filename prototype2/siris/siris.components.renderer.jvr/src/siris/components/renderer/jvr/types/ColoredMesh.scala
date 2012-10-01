package siris.components.renderer.jvr.types

import javax.media.opengl.{GL2GL3, GL}
import de.bht.jvr.core._
import attributes._
import de.bht.jvr.math.Vector3
import simplex3d.math.floatm.Vec3f
import java.nio.{FloatBuffer, IntBuffer}


/**
 * User: dwiebusch
 * Date: 19.04.12
 */

object ColoredMesh{
  def plainRectangle = new ColoredMesh(
    Array[Int](0,1,2, 0,2,3), 6,
    Array[Float](
      -0.5f,-0.5f,0,1,
      0.5f,-0.5f,0,1,
      0.5f, 0.5f,0,1,
      -0.5f, 0.5f,0,1),
    Array[Float](0,0,1, 0,0,1, 0,0,1, 0,0,1),
    Array[Float](1,1,1, 1,1,1, 1,1,1, 1,1,1),
    Some(Array[Float](0,1, 1,1, 1,0, 0,0))
  )


  private def toArray(v : Vec3f) = Array(v.x, v.y, v.z)

  private def data2pos(data : Data) : Array[Float] =
    data.points.flatMap(toArray)

  private def data2normal(data : Data) : Array[Float] =
    data.normals.flatMap(toArray)

  private def data2idx(data : Data) : Array[Int] =
    data.faces.flatMap{ _.foreach( v => data.vertices(v).vertexIdx ) }.toArray

  private def data2color(data : Data) : Array[Float] =
    data.colors.collect{ case colors => data.vertices.flatMap{ v => toArray(colors(v.texIdx)) } }.getOrElse(null)

  def apply(data : Data) : ColoredMesh = {
    val tmp = data2idx(data)
    new ColoredMesh(tmp, tmp.length, data2pos(data), data2normal(data), data2color(data), data.texCoords)
  }
}

class ColoredMesh (i : Array[Int], numIndices : Int, p : Array[Float], n : Array[Float], c : Array[Float], t : Option[Array[Float]] = None)
  extends Geometry
{
  private def this() =
    this(Array[Int](), 0, Array[Float](), Array[Float](), Array[Float]())

  /** The vertexbuffer object. */
  var vbo = new DirectVertexBuffer

  /** The bounding box. */
  var bBox = new BBox()

  /** The vertex positions */
  var positions   = Array[Float]()
  var colors      = Array[Float]()
  var normals     = Array[Float]()
  var texCoords   = Array[Float]()

  /** Vertices are changed */
  var updates     = new java.util.HashMap[String, AttributeUpdate]()
  var needUpdate  = false
  var bBoxDirty   = true


  setData(i, numIndices, p, n, c)

  def setData(data : Data, ni : Int, faces : List[Face]) {
    setData(ColoredMesh.data2idx(data), ni, ColoredMesh.data2pos(data),
      ColoredMesh.data2normal(data), ColoredMesh.data2color(data))
  }

  protected  def setData(indices : Array[Int], ni : Int, pos : Array[Float], nrmls : Array[Float], clrs : Array[Float]){
    setPos(IntBuffer.wrap(indices), ni, pos)
    setNormals(nrmls)
    setTexCoords(t.getOrElse(new Array[Float](positions.length / 4 * 2)))
    if (clrs != null)
      setColors(clrs)
  }

  /*
  * (non-Javadoc)
  * @see de.bht.jvr.core.Geometry#getBBox()
  */
  @Override
  def getBBox = {
    if (bBoxDirty)
      updateBBox()
    bBox
  }


  def getTexCoords =
    texCoords

  /**
   * Gets a index (fast)
   *
   * @param i the i th index
   * @return the index value
   */
  def getIndex(i : Int)  =
    vbo.getIndex(i)

  /**
   * Gets a copy of the indices (slow).
   *
   * @return the indices
   */
  def getIndices =
    vbo.getIndices

  /**
   * Gets the size of the index array (fast)
   *
   * @return the size
   */
  def getIndicesCount =
    vbo.getSize

  def getNormal(n : Int) =
    new Vector3(normals(3*n), normals(3*n+1), normals(3*n+2))

  def getNormalCount =
    normals.length / 3

  def getNormals =
    normals

  @Override
  def getRenderClone = {
    val clone = new ColoredMesh()
    clone.bBox = getBBox
    clone.bBoxDirty = false
    clone.updates = getUpdates
    clone.vbo = vbo
    clone
  }

  private val texCoordUpdate = new AV2()
  private val colorUpdate = new AV3()
  private val normalUpdate = new AV3()
  private val positionUpdate = new AV4()

  private def getUpdates = {
    if (needUpdate) {
      updates.put("jvr_Normal", normalUpdate.update(normals))
      updates.put("jvr_Vertex", positionUpdate.update(positions))
      updates.put("jvr_TexCoord", texCoordUpdate.update(texCoords))
      updates.put("jvr_Vertex_Color", colorUpdate.update(colors))
      needUpdate = false
    }

    val retVal = new java.util.HashMap[String, AttributeUpdate]()
    retVal.putAll(updates)
    retVal
  }

  /**
   * Gets a vertex position (fast).
   *
   * @param n
    *            the n-th vertex
   * @return the vertex position
   */
  def getVertex(n : Int) =
    new Vector3(positions(3*n), positions(3*n+1), positions(3*n+2))

  /**
   * Gets a copy of the vertex positions (slow).
   *
   * @return the positions
   */
  def getVertices =
    positions

  /**
   * Gets the size of the vertices array (fast).
   *
   * @return the size
   */
  def getVerticesCount =
    positions.length / 3

  @Override
  def isBuilt(ctx : Context) =
    vbo.isBuilt(ctx)

  @Override
  def pick(ray : PickRay) = {
    val o = ray.getRayOrigin
    val d = ray.getRayDirection.normalize()
    var closestPoint : Vector3 = null
    var closestDist = 0f

    for (i <- Range(0, vbo.getSize, 3)) {
      var idx = vbo.getIndex(i)
      val p0 = getVertex(idx)
      idx = vbo.getIndex(i + 1)
      val p1 = getVertex(idx)
      idx = vbo.getIndex(i + 2)
      val p2 = getVertex(idx)

      val point = rayTriIntersect(o, d, p0, p1, p2)

      if (point != null) {
        val dist = o.sub(point).length()
        if (closestPoint == null || dist < closestDist) {
          closestPoint = point
          closestDist = dist
        }
      }
    }
    closestPoint
  }

  /**
   * Calculates the intersection of a line and a triangle.
   *
   * @param orig origin of the picking ray
   * @param dir direction of the picking ray
   * @param vert0 first triangle point
   * @param vert1 second triangle point
   * @param vert2 third triangle point
   * @return the intersection point or null
   */
  private def rayTriIntersect(orig : Vector3, dir : Vector3, vert0 : Vector3, vert1 : Vector3, vert2 : Vector3) : Vector3 = {
    // http://www.cs.virginia.edu/~gfx/Courses/2003/ImageSynthesis/papers/Acceleration/Fast%20MinimumStorage%20RayTriangle%20Intersection.pdf

    val epsilon = 0.000001f

    // find vectors for two edges sharing vert0
    val edge1 = vert1.sub(vert0)
    val edge2 = vert2.sub(vert0)

    // begin calculating determinant - also used to calculate U parameter
    val pvec = dir.cross(edge2)

    // if determinant is near zero, ray lies in plane of triangle
    val det = edge1.dot(pvec)

    if (det > -epsilon && det < epsilon)
      return null
    val inv_det = 1f / det

    // calculate distance from vert0 to ray origin
    val tvec = orig.sub(vert0)

    // calculate U parameter and test bounds
    val u = tvec.dot(pvec) * inv_det
    if (u < 0 || u > 1)
      return null

    // prepare to test V parameter
    val qvec = tvec.cross(edge1)

    // calculate V parameter and test bounds
    val v = dir.dot(qvec) * inv_det
    if (v < 0 || u + v > 1)
      return null

    // calculate intersection point
    val e = edge1.cross(edge2)
    val d = e.dot(vert0)
    val s = (d - e.dot(orig)) / e.dot(dir)
    if (s >= 0)
      return orig.add(dir.mul(s))
    null
  }

  /*
  * (non-Javadoc)
  * @see de.bht.jvr.core.Geometry#render(de.bht.jvr.core.Context)
  */
  @Override
  def render(ctx : Context) {
    val program = ctx.getShaderProgram
    if (program != null) {
      vbo.enqueueUpdates(ctx, updates)
      vbo.bind(ctx)

      // draw mesh
      ctx.getGL.glDrawElements(GL.GL_TRIANGLES, vbo.getSize, GL.GL_UNSIGNED_INT, 0)

      vbo.unbind(ctx)
    } else
      throw new Exception("No active shader program to render the mesh.")
  }

  def setAttribute(name : String, values : AttributeValues)  = {
    if (values.getSize != getVerticesCount)
      throw new RuntimeException("Invalid number of list elements: " + values.getSize)

    if (name.equals("jvr_Vertex"))
      throw new RuntimeException(
        "You can't update vertices this way. But you can use the methode setVertices(List<Vector4> positions) instead.")

    // update attribute array
    updates.put(name, new AttributeUpdate(values))
  }

  /**
   * Sets new vertex positions (use this method to update all vertices).
   *
   * @param pos new positions
   */
  def setVertices(indices : IntBuffer, ni : Int, pos : Array[Float]){
    if (indices != null)
      vbo.setIndices(indices, ni)
    else
      throw new Exception("Invalid number of indices in mesh: 0")

    positions  = pos
    bBoxDirty  = true
    needUpdate = true
  }

  def setNormals(nrmls : Array[Float]){
    normals     = nrmls
    needUpdate  = true
  }

  def setColors(cls : Array[Float]){
    colors      = cls
    needUpdate  = true
  }

  def setTexCoords(coords : Array[Float]){
    texCoords   = coords
    needUpdate  = true
  }

  protected def setPos(indices : IntBuffer, ni : Int, pos : Array[Float]){
    if (pos.length % 4 == 0) {
      setVertices(indices, ni, pos)
    } else
      throw new Exception("Invalid number of positions in mesh: " + pos.length)
  }


  /**
   * Creates the bounding box.
   */
  private def updateBBox() {
    bBox = new BBox()
    for (i <- 0  until getVerticesCount)
      bBox = bBox.grow(getVertex(i))
    bBoxDirty = false
  }
}


trait Updateable extends AttributeValues{
  protected var size : Int = 0
  protected def getValues : Array[Float]
  protected def setValues( v : Array[Float] )

  def update(newVal : Array[Float]) : AttributeUpdate =  {
    size = newVal.length
    val oldVal = getValues
    if (oldVal.length < size)
      setValues(java.util.Arrays.copyOf(newVal, size))
    else {
      System.arraycopy(newVal, 0, oldVal, 0, size)
      setValues(oldVal)
    }
    new AttributeUpdate(this)
  }

  override def update(ctx: Context) {
    val gl: GL2GL3 = ctx.getGL
    val buff: FloatBuffer = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY).asFloatBuffer
    buff.put(getValues, 0, size)
    gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER)
  }
}

class AV3 extends AttributeVector3(Array[Float]()) with Updateable{
  protected def setValues(v: Array[Float]) {values = v}
  override def getSize = size / elementSize
  protected def getValues = values

  def this(values: Array[Float]) {
    this()
    elementSize = 3
    update(values)
  }
}

class AV2 extends AttributeVector2(Array[Float]()) with Updateable{
  protected def setValues(v: Array[Float]) {values = v}
  override def getSize = size / elementSize
  protected def getValues = values

  def this(values: Array[Float]) {
    this()
    elementSize = 2
    update(values)
  }
}

class AV4 extends AttributeVector4(Array[Float]()) with Updateable {
  protected def setValues(v: Array[Float]) {values = v}
  override def getSize = size / elementSize
  protected def getValues = values

  def this(values: Array[Float]) {
    this()
    elementSize = 4
    update(values)
  }
}

