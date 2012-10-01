package siris.applications.basicexample

import siris.core.SIRISApplication

import siris.components.worldinterface.WorldInterface
import siris.components.editor.{EditorConfiguration, Editor}
import siris.components.physics.jbullet.JBulletComponent
import siris.components.physics.ImplicitEitherConversion._

import simplex3d.math.floatm.renamed._
import siris.components.physics.{PhysSphere, PhysPlane, PhysicsConfiguration}
import siris.components.naming.NameIt
import simplex3d.math.floatm.ConstVec3f
import siris.ontology.EntityDescription

/*
* Created by IntelliJ IDEA.
* User: martin
* Date: 6/16/11
* Time: 3:06 PM
*/

object PhysicsEditorExample extends SIRISApplication {
  def main( String : Array[String] ) { start() }

  protected def createComponents() {
    println("creating components")

    //create & register components
    WorldInterface.registerComponent(new JBulletComponent('physics))
    PhysicsConfiguration(ConstVec3f(0,-9.81f,0)).deliver()

    WorldInterface.registerComponent(new Editor('editor))
    EditorConfiguration(appName = "MasterControlProgram").deliver()

    // start all registered components
    WorldInterface.getRegisteredComponents.foreach(_._2.start())
  }

  override protected def shutdown() {
    WorldInterface.getRegisteredComponents.foreach(_._2.shutdown())
    WorldInterface.shutdown()
    super.shutdown()
  }

  protected def createEntities() {
    println("creating entities")

    //Create plane
    EntityDescription(
      PhysPlane(
        transform = ConstVec3(0,0,0),
        normal = ConstVec3(0,1,0),
        thickness = 1,
        mass =  0,
        restitution = 1
      ),
      NameIt("Grid")
    ).realize( entity => println("plane ready") )

    //Create a ball above the plane
    EntityDescription(
      PhysSphere(
        transform = ConstVec3(0,10,0),
        radius = 1,
        mass = 1,
        restitution = 1
      ),
      NameIt("The Bit")
    ).realize( entity => println("ball ready") )

  }

  protected def finishConfiguration() {
    println("running")
  }
}