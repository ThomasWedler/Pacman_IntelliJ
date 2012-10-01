package siris.components.physics.simsim

import simplex3d.math.floatm.{Mat3x4f, Vec3f, Mat4x4f}

/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 8/25/11
 * Time: 12:28 PM
 */
class Particle(
  var position: Vec3f = Vec3f.Zero,
  var linearVelocity: Vec3f = Vec3f.Zero,
  var linearAcceleration: Vec3f = Vec3f.Zero,
  var mass: Float = 1f,
  var linearDamping: Float = 1f) {

  def setTransformation(transformation: Mat4x4f) {
    position = transformation(3).xyz
  }

  def getTransformation =
    Mat4x4f(Mat3x4f.translate(position))

  def simulate(deltaT: Float) {

    linearVelocity += linearAcceleration * deltaT
    linearVelocity *= linearDamping
    position += linearVelocity * deltaT
  }

  def getLinearVelocity =
    Vec3f(linearVelocity)

  def setLinearVelocity(newLinearVelocity: Vec3f) {
    linearVelocity = newLinearVelocity
  }

  def getLinearAcceleration =
    linearAcceleration

  def setLinearAcceleration(newLinearAcceleration: Vec3f) {
    linearAcceleration = newLinearAcceleration
  }
}