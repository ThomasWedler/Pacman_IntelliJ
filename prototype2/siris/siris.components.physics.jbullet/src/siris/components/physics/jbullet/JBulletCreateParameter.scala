package siris.components.physics.jbullet

import siris.components.physics.PhysicsAspect
import runtime.RichFloat
import simplex3d.math.floatm.renamed._
import siris.ontology.Symbols
import siris.core.entity.description.SValSeq

//Global Types
import siris.ontology.{types => gt}

/*
 * User: martin
 * Date: 6/8/11
 * Time: 11:34 AM
 */

/**
 *  Used to configure JBullet.
 *
 * @param simulationSpeed A multiplyer to deltaT. If this value is greater than 1.0f the simulation runs faster and vice versa.
 * @param gravity         The global gravity of the simulation.
 */
case class JBulletConfiguration(simulationSpeed: RichFloat = null, gravity: ConstVec3 = null,
                                override val targets : List[Symbol] = Nil) extends PhysicsAspect(Symbols.configuration) {

  override def getFeatures =
    Set( gt.SimulationSpeed, gt.Gravity )


  def getProvidings =
    getFeatures

  def getCreateParams() = addCVars {
    val result = new SValSeq
    if(simulationSpeed != null) result and gt.SimulationSpeed(simulationSpeed.self.asInstanceOf[Float])
    if(gravity != null) result and gt.Gravity(gravity)
    result
  }
}