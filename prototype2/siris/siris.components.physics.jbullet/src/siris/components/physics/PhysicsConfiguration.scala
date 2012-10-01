package siris.components.physics

import siris.core.component.ComponentConfiguration
import siris.ontology.Symbols
import siris.core.entity.description.{SValList, Semantics}
import simplex3d.math.floatm.renamed.ConstVec3

//Global Types
import siris.ontology.{types => gt}

/*
* User: martin
* Date: 6/10/11
* Time: 10:23 AM
*/

/**
 *  Used to configure a PhysicsComponent
 *
 * @param simulationSpeed A multiplyer to deltaT. If this value is greater than 1.0f the simulation runs faster and vice versa.
 * @param gravity         The global gravity of the simulation.
 */
case class PhysicsConfiguration(gravity: ConstVec3, simulationSpeed: Float = 1.0f) extends ComponentConfiguration {

  def toConfigurationParams: SValList =
    new SValList(gt.Gravity(gravity), gt.SimulationSpeed(simulationSpeed))

  def targetComponentType = Symbols.physics
}