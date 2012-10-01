package siris.applications.basicexample

import simplex3d.math.floatm.{Vec3f, ConstVec3f}
import siris.components.renderer.jvr.{JVRInit, JVRConnector}
import siris.components.physics.jbullet.JBulletComponent
import siris.components.worldinterface.WorldInterface
import siris.components.physics.PhysicsConfiguration
import siris.components.renderer.messages._
import siris.core.SIRISApplication
import objects._
import actors.Actor
import siris.core.entity.description.SequentialRealize
import siris.components.editor.{EditorConfiguration, Editor}


/**
 * User: dwiebusch
 * Date: 26.05.11
 * Time: 10:09
 */

object ExampleApplication extends SIRISApplication with JVRInit{
  def main( String : Array[String] ) { start() }

  protected def createComponents() {
    println("creating components")
    // create components
    val physics  = new JBulletComponent('physics)
    val renderer = new JVRConnector('renderer)
    // register components at the WorldInterface
    WorldInterface.registerComponent(renderer)
    WorldInterface.registerComponent(physics)
    // send configs
    renderer ! ConfigureRenderer( Actor.self, BasicDisplayConfiguration(800, 600), true, EffectsConfiguration( "low","none" ) )
    renderer ! RenderNextFrame( Actor.self )
    PhysicsConfiguration (ConstVec3f(0,-9.81f,0)).deliver()

    WorldInterface.registerComponent(new Editor('editor))
    EditorConfiguration(appName = "MasterControlProgram").deliver()
    // register for exit on close:
    exitOnClose(renderer, shutdown _)
    // start all registered components
    WorldInterface.handleRegisteredComponents(_.values.foreach(_.start()))
  }

  override protected def shutdown() {
    WorldInterface.getRegisteredComponents.values.foreach( _.shutdown() )
    WorldInterface.shutdown()
    super.shutdown()
  }

  protected def createEntities() {
    println("creating entities")
    Table("the table", Vec3f(3f, 1f, 2f), Vec3f(0f, -1.5f, -7f)).realize( e => println("created new table") )
    Light("the light", Vec3f(-4f, 8f, -7f), 270f, -25f, 0f).realize( e => println("created new light"))
    Ball ("the ball", 0.2f,  Vec3f(0f, 1f, -7f)).realize( e => println("created new ball") )

    //SequentialRealize( Table("the table", Vec3f(3f, 1f, 2f), Vec3f(0f, -1.5f, -7f)).desc ).whereResultIsProcessedBy( e => println("created new table") ).thenRealize( Light("the light", Vec3f(-4f, 8f, -7f), 270f, -25f, 0f).desc ).whereResultIsProcessedBy( e => println("created new light") ).thenRealize( Ball ("the ball", 0.2f,  Vec3f(0f, 1f, -7f)).desc ).whereResultIsProcessedBy(  e => println("created new ball") ).execute
  }

  protected def finishConfiguration() {
    println("application is running")
  }
}