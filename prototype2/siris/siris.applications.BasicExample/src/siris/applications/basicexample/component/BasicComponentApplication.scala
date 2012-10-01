package siris.applications.basicexample.component

import siris.core.SIRISApplication
import siris.components.renderer.jvr._
import siris.components.worldinterface.WorldInterface
import siris.applications.basicexample.objects.Light
import siris.applications.basicexample.BasicDisplayConfiguration
import siris.components.renderer.jvr.JVRInit
import simplex3d.math.floatm.Vec3f
import actors.Actor
import siris.components.renderer.messages.{RenderNextFrame, ConfigureRenderer, EffectsConfiguration}

/**
 * @author dwiebusch
 * Date: 01.07.11
 * Time: 16:59
 */

object BasicComponentApplication extends SIRISApplication with JVRInit{
  def main( String : Array[String] ) { start() }

  protected def createComponents() {
    println("creating components")
    // create components
    val renderer    = new JVRConnector('renderer)
    val myComponent = new ExampleComponent
    // register components at the WorldInterface
    WorldInterface.registerComponent(renderer)
    WorldInterface.registerComponent(myComponent)
    // send configs
    renderer ! ConfigureRenderer( Actor.self, BasicDisplayConfiguration(800, 600), true, EffectsConfiguration("low","none") )
    renderer ! RenderNextFrame( Actor.self )
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
    Light("the light", Vec3f(-4f, 8f, -2f), 270f, -25f, 0f).realize( e => println("created new light"))
    ExampleBall ("the ball", 0.2f,  Vec3f(0f, 0f, -3f)).realize( e => println("created new ball") )
  }

  protected def finishConfiguration() {
    println("application is running")
  }
}