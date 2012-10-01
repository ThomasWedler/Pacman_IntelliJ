package siris.components.physics.jbullet

import siris.core.entity.Entity
import siris.components.physics.PhysicsMessage

/*
 * User: martin
 * Date: 6/10/11
 * Time: 2:47 PM
 */

/**
 * Tells JBullet to make the entity static at its current position.
 */
case class JBHold(entity: Entity) extends PhysicsMessage

/**
 * Tells JBullet to release a prevously held entity using its stored mass.
 */
case class JBRelease(entity: Entity) extends PhysicsMessage

/**
 * Tells JBullet to simulate a new frame.
 */
private[jbullet] case class JBSimulateFrame()
