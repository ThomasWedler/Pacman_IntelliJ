package siris.components.physics

import siris.core.entity.Entity
import simplex3d.math.floatm.{Mat3x4f, ConstVec3f, ConstMat4f}
import actors.Actor

/*
* User: martin
* Date: 6/10/11
* Time: 11:17 AM
*/

/**
 *  Base class for all physics messages.
 */
abstract class PhysicsMessage

/**
 *  Tells a physics component to set the transformation of an entity.
 */
case class SetTransformation(e: Entity, t: ConstMat4f) extends PhysicsMessage

/**
 *  Tells a physics component to set the position of an entity.
 */
object SetPosition{
  def apply(e: Entity, p: ConstVec3f) = SetTransformation(e, ConstMat4f(Mat3x4f.translate(p)))
}

/**
 *  Tells a physics component to apply an impulse to an entity.
 */
case class ApplyImpulse(e: Entity, i: ConstVec3f) extends PhysicsMessage

/**
 *  Tells a physics component to apply an torque impulse to an entity.
 */
case class ApplyTorqueImpulse(e: Entity, i: ConstVec3f) extends PhysicsMessage

/**
 *  Tells a physics component to set the linear velocity of an entity.
 */
case class SetLinearVelocity(e: Entity, v: ConstVec3f) extends PhysicsMessage

/**
 *  Tells a physics component to set the angular velocity of an entity.
 */
case class SetAngularVelocity(e: Entity, v: ConstVec3f) extends PhysicsMessage

/**
 *  Tells a physics component to set the gravity of an entity.
 */
case class SetGravity(e: Entity, g: ConstVec3f) extends PhysicsMessage

/**
 *  Tells a physics component to temporary detache e from the physical simulation,
 *        until it is reattached using the AttachEntity message.
 * @see   AttachEntity
 */
case class DetachEntity(e: Entity)

/**
 *  Tells a physics component to reattach an entity that was previously
 *        detached from physical simulation using DetachEntity
 * @see   DetachEntity
 */
case class AttachEntity(e: Entity)

/**
 *    Holds multiple PhysicsMessages.
 * Used to send multiple messages to a physics component
 *          that have to be handled uninterruptedly in the given order.
 */
case class BunchOfPhysicsMessages(msgs: List[PhysicsMessage]) extends PhysicsMessage