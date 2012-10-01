/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/11/11
 * Time: 11:39 AM
 */
package siris.components.physics.jbullet

import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.collision.broadphase.{BroadphaseInterface, Dispatcher}
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver
import com.bulletphysics.collision.dispatch.CollisionConfiguration
import javax.vecmath.Vector3f

class JBulletDiscreteDynamicsWorld(dispatcher: Dispatcher, pairCache: BroadphaseInterface, constraintSolver: ConstraintSolver, collisionConfiguration: CollisionConfiguration)
  extends DiscreteDynamicsWorld(dispatcher, pairCache, constraintSolver, collisionConfiguration){

  def getGravity(): Vector3f = {
    val g = new Vector3f
    getGravity(g)
    g
  }
}


