package siris.components.renderer.jvr.examples

import siris.core.SIRISApplication
import siris.components.worldinterface.WorldInterface
import siris.components.renderer.createparameter.{PointLight, ShapeFromFile}
import simplex3d.math.floatm.renamed._
import siris.components.renderer.jvr.{OntologyJVR, JVRConnector, JVRInit}
import de.bht.jvr.collada14.CommonColorOrTextureType.Color
import actors.Actor
import siris.components.renderer.messages.RenderNextFrame._
import siris.components.renderer.messages.{RenderNextFrame, ConfigureRenderer, EffectsConfiguration}

/**
 * Created by IntelliJ IDEA.
 * User: stephan_rehfeld
 * Date: 18.07.11
 * Time: 09:40
 * To change this template use File | Settings | File Templates.
 */

object BumpMappingExamples extends SIRISApplication with JVRInit {

  def main( String : Array[String] ) { start() }

  protected def createComponents() {
    println("creating components")
    // create components
    val renderer = new JVRConnector('renderer)
    // register components at the WorldInterface
    WorldInterface.registerComponent(renderer)
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

    /*val shaderEffect = ShaderEffect withPasses "AMBIENT" and "LIGHING" where
      RenderPass "AMBIENT" usesFiles ("data/shader/ambient.vs" :: "data/shader/ambient.fs" :: Nil ) hasVariables
        "jvr_Material_Ambient" hasValue new Color( 0.0f, 0.0f, 0.0f, 1.0f ) isReachableBy EINONTOLOGYSYMBOL,
      RenderPass "LIGHTING" usesFiles ("data/shader/bumpmapping.vs" :: "data/shader/bumpmapping.fs" :: Nil) hasVariables
        "jvr_Material_Diffuse" hasValue new Color( 0.7f, 0.6f, 0.18f, 1.0f ) and
        "jvr_Material_Specular" hasValue new Color( 0.7f, 0.6f, 0.18f, 1.0f ) and
        "jvr_Material_Shininess" hasValue 6f pack

    val teapot = new EntityDescription(
      new ShapeFromFile(
        file = "data/meshes/teapot.dae"

      )
    ).realize()

    val pointlight = new EntityDescription(
      new PointLight(
        name = "pointlight",
        transformation = ConstMat4( Mat3x4.translate( Vec3( 0, 5, 10 ) ) )
      )
    ).realize() */

  }

  protected def finishConfiguration() {
    println("application is running")
  }

}