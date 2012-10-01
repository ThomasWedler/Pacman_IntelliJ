package siris.components.physics

import siris.core.component.Component
import siris.components.eventhandling.{EventHandler, EventProvider}
import siris.ontology.Symbols
import siris.core.entity.Entity
import siris.core.entity.description.SVal

//Global Types
import siris.ontology.{types => gt}

/*
* Created by IntelliJ IDEA.
* User: martin
* Date: 6/10/11
* Time: 10:30 AM
*/

/**
 *  Base trait for all physics components
 */
trait PhysicsComponent extends Component with EventProvider with EventHandler {
  final def componentType = Symbols.physics

  /**
   *  Sets the transformation of an entity.
   *
   * @param e The entity
   * @param t The transformation
   *
   * @see   [[siris.components.physics.PhysicsMessage]]
   */
  def handleSetTransformation(e: Entity, t: SVal[gt.Transformation.dataType])
  addHandler[SetTransformation]{msg => handleSetTransformation(msg.e, gt.Transformation(msg.t))}

  /**
   *  Applies an impules to an entity
   *
   * @param e The entity
   * @param i The impulse
   *
   * @see   [[siris.components.physics.PhysicsMessage]]
   */
  def handleApplyImpulse(e: Entity, i: SVal[gt.Impulse.dataType])
  addHandler[ApplyImpulse]{msg => handleApplyImpulse(msg.e, gt.Impulse(msg.i))}

  /**
   *  Applies an impules to an entity
   *
   * @param e The entity
   * @param i The impulse
   *
   * @see   [[siris.components.physics.PhysicsMessage]]
   */
  def handleApplyTorqueImpulse(e: Entity, i: SVal[gt.Impulse.dataType])
  addHandler[ApplyTorqueImpulse]{msg => handleApplyTorqueImpulse(msg.e, gt.Impulse(msg.i))}

  /**
   *  Sets the linear velocity of an entity.
   *
   * @param e The entity
   * @param v The linear velocity
   *
   * @see   [[siris.components.physics.PhysicsMessage]]
   */
  def handleSetLinearVelocity(e: Entity, v: SVal[gt.Velocity.dataType])
  addHandler[SetLinearVelocity]{msg => handleSetLinearVelocity(msg.e, gt.Velocity(msg.v))}

  /**
   *  Sets the angular velocity of an entity.
   *
   * @param e The entity
   * @param v The angular velocity
   *
   * @see   [[siris.components.physics.PhysicsMessage]]
   */
  def handleSetAngularVelocity(e: Entity, v: SVal[gt.Velocity.dataType])
  addHandler[SetAngularVelocity]{msg => handleSetAngularVelocity(msg.e, gt.Velocity(msg.v))}

  /**
   *  Sets the gravity of an entity.
   *
   * @param e The entity
   * @param g The gravity
   *
   * @see   [[siris.components.physics.PhysicsMessage]]
   */
  def handleSetGravity(e: Entity, g: SVal[gt.Gravity.dataType])
  addHandler[SetGravity]{msg => handleSetGravity(msg.e, gt.Gravity(msg.g))}

  /**
   *  Detaches e from the physical simulation,
   *        until it is reattached using the handleAttachEntity message.
   *
   * @param e The entity
   *
   * @see   [[siris.components.physics.PhysicsMessage]]
   */
  def handleDetachEntity(e: Entity)
  addHandler[DetachEntity]{msg => handleDetachEntity(msg.e)}

  /**
   *  Reattachs an entity that was previously
   *        detached from physical simulation using handleDetachEntity
   *
   * @param e The entity
   *
   * @see   [[siris.components.physics.PhysicsMessage]]
   */
  def handleAttachEntity(e: Entity)
  addHandler[AttachEntity]{msg => handleAttachEntity(msg.e)}

  /**
   * Handle all messages.
   */
  addHandler[BunchOfPhysicsMessages]{msg => msg.msgs.foreach( applyHandlers(_) )}

}