/*
   * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/5/11
 * Time: 2:12 PM
 */
package siris.components.physics.jbullet

import com.bulletphysics.linearmath.Transform
import javax.vecmath.Matrix4f
import javax.vecmath.Vector3f

object JBulletMath {

  /**
   *  Returns a transformation with no scaling (= 1.0)
   *        Does not alter the passed Transform t
   */
  def removeScale(t: Transform): Transform = {
    var tmp = new Vector3f
    val scale = for(i <- 0 until 3) yield {t.basis.getRow(i, tmp); tmp.length}
    val copy = new Transform(t)
    copy.mul(new Transform(ScaleMat(1.0f/scale(0), 1.0f/scale(1), 1.0f/scale(2))))
    copy
  }

  /**
   *  Creates a Idendity Transform
   */
  def Idendity = new Transform(IdendityMat)

  /**
   *  Creates a new scale matrix
   */
  def ScaleMat(x: Float, y: Float, z: Float): Matrix4f =
    new Matrix4f(x,0,0,0,0,y,0,0,0,0,z,0,0,0,0,1)

  /**
   *  Creates a new scale matrix
   */
  def ScaleMat(s: Float): Matrix4f = ScaleMat(s,s,s)

  /**
   *  Returns a new idendity matrix
   */
  def IdendityMat() = ScaleMat(1)

  /**
   *  Multiplies a vector with a scalar
   */
  def mul(v: Vector3f, s: Float) =
    new Vector3f(v.x * s , v.y * s, v.z * s)

  /**
   *  Returns a Vector with x, y, z = 0
   */
  def ZeroVec = new Vector3f(0,0,0)

  /**
   *   Runs some tests
   */
  def main(args: Array[String]): Unit = {

    println(new Matrix4f)
    val mat = new Matrix4f
    mat.setScale(3)
    println(mat)
    println(ScaleMat(3))
    println(ScaleMat(1,2,3))

    var tmp = new Matrix4f
    removeScale(new Transform(ScaleMat(3))).getMatrix(tmp)
    println(tmp)
    removeScale(new Transform(ScaleMat(1,2,3))).getMatrix(tmp)
    println(tmp)
  }
}