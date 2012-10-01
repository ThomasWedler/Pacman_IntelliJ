package siris.components.physics.jbullet

import javax.vecmath.{Matrix4f, Vector3f}
import siris.core.entity.typeconversion.{ConvertibleTrait, Converter}
import simplex3d.math.floatm.{Mat3x4f, Vec3f, Mat4x4f}

//Global Types & Local Types
import siris.components.physics.jbullet.{types => lt}
import siris.ontology.{types => gt}

/*
* Created by IntelliJ IDEA.
* User: martin
* Date: 6/7/11
* Time: 10:32 AM
*/
object JBulletConverters {

  //Todo:
  //This encounters a jbullet bug:
  //When absolute sizes and positions get too small < 0.1f, jbullet does not compute correct values any more
  //This scale factor i used to magnify all values passed to jbullet
  //But this hase to be solved in a nicer way
  //
  //!!! Be careful when changeing scale to a value other than 1f. !!!
  //!!! This hotfix does currently not support to scale shapes from collada files !!!
  //It is likely that I (martin) forgot to check all locations where scale has to be multiplied
  var scale = 1f

  val vectorConverter = new Converter[Vector3f, Vec3f] {
    override def canRevert(to: ConvertibleTrait[_], from: ConvertibleTrait[_]) =
      to.typeinfo == lt.Vector3.typeinfo && from.typeinfo == gt.Vector3.typeinfo

    override def canConvert(from: ConvertibleTrait[_], to: ConvertibleTrait[_]) =
      to.typeinfo == gt.Vector3.typeinfo && from.typeinfo == lt.Vector3.typeinfo

    def revert(from: Vec3f): Vector3f =
      new Vector3f(from.x*scale, from.y*scale, from.z*scale)

    def convert(from: Vector3f): Vec3f =
      Vec3f(from.x, from.y, from.z) * (1f/scale)
  }

  val transformConverter = new Converter[com.bulletphysics.linearmath.Transform, simplex3d.math.floatm.renamed.Mat4x4] {
    override def canRevert(to: ConvertibleTrait[_], from: ConvertibleTrait[_]) =
      to.typeinfo == lt.Transformation.typeinfo && from.typeinfo == gt.Transformation.typeinfo

    override def canConvert(from: ConvertibleTrait[_], to: ConvertibleTrait[_]) =
      from.typeinfo == lt.Transformation.typeinfo && to.typeinfo == gt.Transformation.typeinfo

    def revert(from: simplex3d.math.floatm.renamed.Mat4x4): com.bulletphysics.linearmath.Transform = {
      new com.bulletphysics.linearmath.Transform(
        new Matrix4f(
          from.m00, from.m01, from.m02, from.m03 * scale,
          from.m10, from.m11, from.m12, from.m13 * scale,
          from.m20, from.m21, from.m22, from.m23 * scale,
          from.m30, from.m31, from.m32, from.m33)
      )
    }

    def convert(from: com.bulletphysics.linearmath.Transform): simplex3d.math.floatm.renamed.Mat4x4 = {
      val matrix = new Matrix4f
      from.getMatrix(matrix)
      simplex3d.math.floatm.renamed.Mat4x4(
        matrix.m00, matrix.m10, matrix.m20, matrix.m30,
        matrix.m01, matrix.m11, matrix.m21, matrix.m31,
        matrix.m02, matrix.m12, matrix.m22, matrix.m32,
        matrix.m03*(1f/scale), matrix.m13*(1f/scale), matrix.m23*(1f/scale), matrix.m33)
    }
  }

  def register() {}
}