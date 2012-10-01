package siris.components.renderer.jvr.types

import java.nio.IntBuffer
import java.util.Collections
import javax.media.opengl.{GL, GL2GL3}
import de.bht.jvr.core.attributes.{AttributeArray, AttributeUpdate}
import de.bht.jvr.core.{Context, ContextValueMap, ShaderProgram}
import com.jogamp.common.nio.Buffers


class DirectVertexBuffer {
  /** The triangle mesh is built. */
  private val built = new ContextValueMap[scala.Boolean](false)
  /** The gl indices id. */
  private val indicesId = new ContextValueMap[Int](-1)
  /** The indices. */
  private var indices = IntBuffer.allocate(0)
  private var numIndices = 0
  /** The attribute arrays. */
  private val attribArrays = Collections.synchronizedMap(new java.util.HashMap[String, AttributeArray])
  private val activeAttribArrays = new ContextValueMap[java.util.Map[String, AttributeArray]]
  private val updates = new ContextValueMap[java.util.Map[String, AttributeUpdate]]

  private def applyUpdates(ctx: Context) {
    val u: java.util.Map[String, AttributeUpdate] = updates.get(ctx)
    if (u != null) {
      val attribArrays = activeAttribArrays.get(ctx)
      import scala.collection.JavaConversions._
      for (entry <- u.entrySet) {
        val name: String = entry.getKey
        val update: AttributeUpdate = entry.getValue
        var arr: AttributeArray = attribArrays.get(name)
        if (arr != null) arr.setUpdate(ctx, update)
        else {
          arr = update.createAttributeArray(ctx)
          attribArrays.put(name, arr)
        }
      }
    }
  }

  def bind(ctx: Context) {
    val gl: GL2GL3 = ctx.getGL
    if (!isBuilt(ctx)) build(ctx)
    val program: ShaderProgram = ctx.getShaderProgram
    if (program != null) {
      applyUpdates(ctx)
      gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indicesId.get(ctx))
      import scala.collection.JavaConversions._
      for (entry <- activeAttribArrays.get(ctx).entrySet) program.bindAttribArray(ctx, entry.getKey, entry.getValue)
    }
    else throw new Exception("No active shader program to bind vbo.")
  }

  /**
   * Builds the vertexbuffer object.
   *
   * @param ctx
     * the context
   * @throws Exception
     * the exception
   */
  def build(ctx: Context) {
    val gl: GL2GL3 = ctx.getGL
    built.put(ctx, false)
    val vboId: Array[Int] = new Array[Int](1)
    gl.glGenBuffers(1, vboId, 0)
    indicesId.put(ctx, new java.lang.Integer(vboId(0)))
    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, indicesId.get(ctx))
    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, getSize * Buffers.SIZEOF_INT, indices, GL.GL_STATIC_DRAW)
    val attribArrays: java.util.Map[String, AttributeArray] = new java.util.HashMap[String, AttributeArray]
    activeAttribArrays.put(ctx, attribArrays)
    import scala.collection.JavaConversions._
    for (entry <- this.attribArrays.entrySet) {
      if (!entry.getValue.isBuilt(ctx)) entry.getValue.build(ctx)
      attribArrays.put(entry.getKey, entry.getValue)
    }
    built.put(ctx, true)
  }

  def enqueueUpdates(ctx: Context, updates: java.util.Map[String, AttributeUpdate]) {
    if (updates != null) {
      var u = this.updates.get(ctx)
      if (u == null) {
        u = new java.util.HashMap[String, AttributeUpdate] ()
        this.updates.put(ctx, u)
      }
      u.putAll(updates)
    }
  }

  protected override def finalize() {
    val ctxList = built.getContextList
    import scala.collection.JavaConversions._
    for (ctx <- ctxList) if (isBuilt(ctx)) ctx.deleteVbo(indicesId.get(ctx))
  }

  def getIndex(i: Int) =
    indices.get(i)

  def getIndices =
    indices

  def getSize =
    numIndices

  /**
   * Checks if the vbo is built.
   *
   * @param ctx
     * the context
   * @return true, if is built
   */
  def isBuilt(ctx: Context) =
    built.get(ctx)


  def setIndices(indices: IntBuffer, numIndices : Int) {
    this.numIndices = numIndices
    this.indices = indices
    for( i <- 0 until built.getContextList.size() )
      built.put(built.getContextList.get(i), false)
  }

  /**
   * Sets a vertex attribute array.
   *
   * @param attributeName
     * the attribute name
   * @param attribArray
     * the attribute array
   */
  def setVertexAttribArray(attributeName: String, attribArray: AttributeArray) {
    if (built.getSize == 0) attribArrays.put(attributeName, attribArray)
    else throw new RuntimeException("VBO have already been initialized!")
  }

  def unbind(ctx: Context) {
    if (isBuilt(ctx)) {
      val gl: GL2GL3 = ctx.getGL
      val program: ShaderProgram = ctx.getShaderProgram
      if (program != null) {
        import scala.collection.JavaConversions._
        for (entry <- activeAttribArrays.get(ctx).entrySet) program.unbindAttribArray(ctx, entry.getKey, entry.getValue)
        gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0)
      }
      else throw new Exception("No active shader program to unbind vbo.")
    }
  }
}
