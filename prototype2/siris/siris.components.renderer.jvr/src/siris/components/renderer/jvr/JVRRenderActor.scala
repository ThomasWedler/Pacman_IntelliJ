package siris.components.renderer.jvr


import de.bht.jvr.core._
import actors.Actor
import pipeline.{PipelineCommandPtr, Pipeline}
import siris.core.entity.Entity
import de.bht.jvr.renderer._
import de.bht.jvr.util.Color
import siris.components.renderer.messages._
import siris.core.entity.description._
import uniforms._
import simplex3d.math.floatm.renamed._
import simplex3d.math.floatm.FloatMath._
import de.bht.jvr.input.{MouseEvent, MouseListener, KeyEvent, KeyListener}
import siris.components.worldinterface.WorldInterface
import siris.core.svaractor.{SVarImpl, SVar, SVarActorLW}
import siris.components.renderer.createparameter.ElementManipulator
import siris.components.renderer.jvr.types.ColoredMesh

//import ext.components.animation.{Animation, AnimationGeometry}
import de.bht.jvr.math.Vector4
import siris.ontology.{types => gt}
import siris.ontology.{SVarDescription, Symbols}
import simplex3d.math.floatm.Vec2f
import siris.core.entity.component.Removability
import siris.core.helper.{ TimeMeasurement, SVarUpdateFunctionMap}
import javax.media.opengl.{GL2ES2, GL2GL3}
import java.io.{FileInputStream, File}

object StereoType extends Enumeration {

  val AnaglyphStereo = Value( "AnaglyphStereo" )

  val FrameSequential = Value( "FrameSequential" )

}


/**
 * This message represents a configuration for a JVRRenderActor
 *
 * @param sender The sender of the configuration
 * @param id The id of the render actor.
 * @param hardwareHandle An optional hardware handle, a tupel of display, screen, and channel. If the value is none the OS descrides where the window will be opened.
 * @param resolution A optional resolution of the window. If not set the window will be opened in full screen.
 * @param size The size of the window.
 * @param transformation The transformation of the view panel relative to the view platform.
 * @param eyeToRender The eye that should be renderer on this window
 * @param autoRender The flag if the actors should render by it self or wait for a RenderNextFrame Message.
 */
case class RenderActorConfig( sender: Actor, id: Symbol, hardwareHandle: Option[(Int,Int,Int)], resolution: Option[(Int, Int)], size: (Double, Double), transformation: ConstMat4, eyeToRender : EyeToRender.Value, autoRender : Boolean, stereoType : Option[StereoType.Value] ) extends RendererMessage

case class RenderActorConfigs( configs : List[RenderActorConfig] )

/**
 * This message is send by the JVRRenderActor if a window has been closed.
 *
 * @param sender The sender of the message.
 */
case class JVRRenderWindowClosed( sender: Actor ) extends RendererMessage

case class JVRPublishUserEntity( sender: Actor, user: Entity ) extends RendererMessage

/**
 * This class represents a OntologyJVR Render actor. One render actor typically manages one display group.
 */
class JVRRenderActor( id: Int, shadowQuality : String, mirrorQuality : String )
  extends SVarActorLW with SVarUpdateFunctionMap with TimeMeasurement /*with WorkloadMonitor*/ {

  /*override def wm_name = "JVR"*/

  override def WakeUpMessage: Any = RenderNextFrame(this)
  framePeriod = 16L * 1000L * 1000L

  val shadows = !shadowQuality.equals( "none" )

  info("Creating" )
  var entityToNodeMap : Map[Entity, GroupNode] = Map()
  var entityToTexMap : Map[Entity, Texture2D] = Map()
  var entityToGeoMap : Map[Entity, ColoredMesh] = Map()

  var lastFrame = System.nanoTime()
  var stereoType : Option[StereoType.Value] = None

  trace( "Creating scene graph root." )
  /**
   * The scene root.
   */
  val sceneRoot = new GroupNode("Root")

  trace( "Creating map for cameras" )
  /**
   * A map of the cameras. Typically one for each window and /or eye.
   */
  var cameras = Map[RenderWindow,Map[Symbol,VRCameraNode]]()


  trace( "Creating list for cameras" )
  /**
   * A list of all cameras.
   */
  var camList = Map[RenderWindow,List[VRCameraNode]]()

  trace( "Creating map for mirror cameras" )
  /**
   * The mirror camreas.
   */
  var mirrorCameras = Map[RenderWindow,Map[VRCameraNode,Map[Symbol,VRCameraNode]]]()

  trace( "Creating list for camera updater" )
  /**
   * This list contains al update object that updates mirror cameras to keep them in sync with the reference camera
   * and the effect plane.
   */
  var cameraUpdater = List[CameraUpdater]()

  trace( "Creating map for skyboxes" )
  /**
   * The sky boxes of the cameras.
   */
  var skyboxes : Map[VRCameraNode,SceneNode] = Map()
  var skyBoxCreater : Option[SkyBoxCreater] = None

  trace( "Creating list for windows" )
  /**
   * The render windows managed my this render actor.
   */
  var windows : List[RenderWindow] = List()

  /**
   * The viewer that controls the render windows.
   */
  var viewer : Viewer = null

  /**
   * The auto render flag.
   */
  var autoRender = false

  /**
   * The keyboard entity.
   */
  // ToDO: I Must be an option, not a null value!
  var keyboardEntity : Entity = null

  // ToDO: I Must be an option, not a null value!
  var mouseEntity : Entity = null



  trace( "Creating list for pipelines" )
  /**
   * The render pipelines of the manages windows.
   */
  var pipelines : Map[RenderWindow,Pipeline] = Map()

  var ambientUniforms : Map[RenderWindow, PipelineCommandPtr] = Map()

  /**
   * This flag indicates if the renderer is paused or not.
   */
  var paused = false

  /**
   * This list contains all observer who get notified when a new frame is rendererd.
   */
  var renderStepObserver : List[Actor] = List()

  /**
   * This list contains the names of all effect planes like mirros, water and portals.
   */
  var effectplanes : List[String] = List()

  /**
   * This list contains all water materials.
   */
  var waterMats : Map[String,ShaderMaterial] = Map()

  /**
   * The time where the actor has been created. Used for the computation of some effects.
   */
  val t0 = System.nanoTime


  var knownEntities : Set[Entity] = Set()

  var postProcessingEffects : List[PostProcessingEffect] = List()
  var entityToPostProcessingEffects : Map[Entity,List[PostProcessingEffect]] = Map()

  /**
   *  Helper function that is used for observer of the view platform transofrm. Sets the transform to all cameras.
   *
   * @param transform The new transformation of the view platform.
   */
  def setTransform( transform : Transform ) {
    for( (window,c) <- camList ) {
      for( camera <- c ) {
        camera.setTransform( transform )
        skyboxes.get( camera ) match {
          case Some( s ) => s.setTransform( camera.getEyeWorldTransform( sceneRoot ).extractTranslation )
          case None =>
        }
      }
    }

  }

  /**
   * Helper function that is used for observer of the head transofrm. Sets the head transform to all cameras.
   *
   * @param transform The new head transformation
   */
  def setHeadTransform( transform : Transform ) {
    for( (window,c) <- camList ) {
      for( camera <- c ) {
        camera.setHeadTransform( transform )
        skyboxes.get( camera ) match {
          case Some( s ) => s.setTransform( camera.getEyeWorldTransform( sceneRoot ).extractTranslation )
          case None =>
        }
      }
    }
    for( updater <- cameraUpdater ) updater.update()
    for( (window,c) <- camList ) {
      for( camera <- c ) {
        skyboxes.get( camera ) match {
          case Some( s ) => s.setTransform( camera.getEyeWorldTransform( sceneRoot ).extractTranslation )
          case None =>
        }
      }
    }
  }

  /**
   * Helper variable to be used in handler.
   */
  var me = this

  addHandler[DetachObject]{
    case msg =>
      val entity = entityToNodeMap( msg.e )
      entity.getParentNode.asInstanceOf[GroupNode].removeChildNode( entity )

  }

  addHandler[AttachObject]{
    case msg =>
      val entity = entityToNodeMap( msg.e )
      sceneRoot.addChildNode( entity )


  }

  addHandler[RegroupEntity] {
    case (RegroupEntity( sender, e, target, convertTransform ) ) =>
      val t  = {
        target match {
          case Some( entity ) => entityToNodeMap( entity )
          case None => sceneRoot }}

      if( convertTransform ) {
        val worldTransform = entityToNodeMap( e ).getWorldTransform( sceneRoot )
        val worldTransformOfTarget = t.getWorldTransform( sceneRoot )
        val newTransform = worldTransformOfTarget.invert.mul( worldTransform )
        entityToNodeMap( e ).setTransform( newTransform )
        e.get( siris.components.renderer.jvr.types.Transformation ).get.set( newTransform )
      }

      entityToNodeMap( e ).getParentNode.asInstanceOf[GroupNode].removeChildNode( entityToNodeMap( e ) )
      t.addChildNode( entityToNodeMap( e ) )
      sender ! RegroupApplied(e, target)

  }

  private var userEntityIsRegistered = false
  addHandler[JVRPublishUserEntity] {
    case msg: JVRPublishUserEntity =>
      if(!userEntityIsRegistered){
        val viewPlatform = msg.user.get(siris.components.renderer.jvr.types.ViewPlatform).get
        val headTransform = msg.user.get(siris.components.renderer.jvr.types.HeadTransform).get

        viewPlatform.observe( setTransform _ )
        viewPlatform.get( setTransform )
        headTransform.observe( setHeadTransform _ )
        headTransform.get( setHeadTransform )
        userEntityIsRegistered = true
      }
  }

  addHandler[RequestTransformation]{
    case msg =>
      println( "Received transformation request" )
      val n : SceneNode = Finder.find( sceneRoot, classOf[SceneNode], msg.element )
      val worldTransform = n.getWorldTransform( sceneRoot )
      msg.sender ! TellTransformation( Actor.self, msg.entity, worldTransform )
  }

  addHandler[RenderActorConfigs] {
    case configs : RenderActorConfigs =>
      var winId = 0
      for( config <- configs.configs) {
        info("Got configuration and configuring")
        var window : RenderWindow = null
        val pipeline = new Pipeline( sceneRoot )

        trace( "Creating render window" )
        config.hardwareHandle match {
          case Some((display,screen,channel) ) =>

            config.resolution match {
              case Some((width,height)) => window = new AwtRenderWindow( pipeline, width, height )
              case None => window = new AwtRenderWindow( pipeline, true )
            }
            println("setscreen " + screen)
            window.setScreenDevice( screen )

          case None =>
            config.resolution match {
              case Some((width,height)) => window = new AwtRenderWindow( pipeline, width, height )
              case None => window = new AwtRenderWindow( pipeline, true )
            }

        }
        window.setVSync( false )
        config.stereoType match {
          case Some( st ) =>
            println ( "Enabling Stereo " + st.equals( StereoType.FrameSequential )  )
            window.setStereo( st.equals( StereoType.FrameSequential ))
          case None => window.setStereo( false )
        }
        cameras = cameras + (window -> Map())
        mirrorCameras = mirrorCameras + (window -> Map())
        camList = camList + (window -> List())
        if( config.eyeToRender == EyeToRender.Both ) {
          println( "Both eyes" )
          cameras = cameras + (window ->  ( cameras( window ) + ('standardLeft -> new VRCameraNode( config.id.toString + "_left", JVRConnector.transformConverter.revert( config.transformation ), new Vector4( (-config.size._1/2).asInstanceOf[Float], (config.size._1/2).asInstanceOf[Float], (config.size._2/2).asInstanceOf[Float], (-config.size._2/2).asInstanceOf[Float] ), true, new Transform ) ) ) )
          cameras = cameras + (window ->  ( cameras( window ) + ('standardRight -> new VRCameraNode( config.id.toString + "_right", JVRConnector.transformConverter.revert( config.transformation ), new Vector4( (-config.size._1/2).asInstanceOf[Float], (config.size._1/2).asInstanceOf[Float], (config.size._2/2).asInstanceOf[Float], (-config.size._2/2).asInstanceOf[Float] ), false, new Transform ) ) ) )
          cameras( window )( 'standardLeft ).setEyeSeparation( -0.07f )
          cameras( window )( 'standardRight ).setEyeSeparation( -0.07f )
        } else {
          println("One eye")
          cameras = cameras + (window -> ( cameras( window ) + ('standard -> new VRCameraNode( config.id.toString(), JVRConnector.transformConverter.revert( config.transformation ), new Vector4( (-config.size._1/2).asInstanceOf[Float], (config.size._1/2).asInstanceOf[Float], (config.size._2/2).asInstanceOf[Float], (-config.size._2/2).asInstanceOf[Float] ), config.eyeToRender == EyeToRender.Left, new Transform ) ) ) )
          cameras(window)('standard).setEyeSeparation(0.0f)
        }

        //        config.viewPlatformSVar.get( setTransform )
        //        config.viewPlatformSVar.observe( setTransform )
        //        config.headTransformSVar.get( setHeadTransform )
        //        config.headTransformSVar.observe( setHeadTransform )

        if( cameras( window ).contains( 'standard ) ) {
          sceneRoot.addChildNode( cameras( window )( 'standard ) )
          camList = camList + (window -> (camList(window) ::: cameras( window )( 'standard ) :: Nil))
        } else {
          sceneRoot.addChildNode( cameras( window )( 'standardLeft ) )
          sceneRoot.addChildNode( cameras( window )( 'standardRight ) )
          camList = camList + (window -> (camList(window) ::: cameras( window )( 'standardLeft ) :: Nil))
          camList = camList + (window -> (camList(window) ::: cameras( window )( 'standardRight ) :: Nil))
          stereoType = config.stereoType
        }
        trace( "Creating pipeline" )

        createDefaultPipeline( pipeline, cameras( window ), sceneRoot, window )

        pipelines = pipelines + (window -> pipeline )

        autoRender = config.autoRender
        var kb = new Entity

        debug( "Constructing keyboad entity" )
        val keyMap = scala.collection.mutable.Map[Char, SVar[Boolean]]()
        for (key <- 'a' to 'z'){
          keyMap.put(key, SVarImpl( false ) )
          kb = kb.injectSVar( keyMap(key), charToKey(key))
        }
        kb = kb.injectSVar( SVarImpl( false ), siris.ontology.types.Key_Right )
        kb = kb.injectSVar( SVarImpl( false ), siris.ontology.types.Key_Left )
        kb = kb.injectSVar( SVarImpl( false ), siris.ontology.types.Key_Up )
        kb = kb.injectSVar( SVarImpl( false ), siris.ontology.types.Key_Down )
        kb = kb.injectSVar( SVarImpl( false ), siris.ontology.types.Key_Space )



        debug( "Registring keyboard listener" )
        window.addKeyListener( new KeyListener {
          override def keyTyped(p1: KeyEvent) {

            keyMap get Character.toLowerCase( p1.getKeyChar ) collect { case svar => svar.set(true) }

          }

          override def keyReleased(p1: KeyEvent) {
            if( p1.getKeyCode == 37 ) {
              kb.get(  siris.ontology.types.Key_Left ).get.set( false )
            } else if( p1.getKeyCode == 39 ) {
              kb.get(  siris.ontology.types.Key_Right ).get.set( false )
            } else if( p1.getKeyCode == 38 ) {
              kb.get(  siris.ontology.types.Key_Up ).get.set( false )
            } else if( p1.getKeyCode == 40 ) {
              kb.get(  siris.ontology.types.Key_Down ).get.set( false )
            } else if( p1.getKeyCode == 32 ) {
              kb.get(  siris.ontology.types.Key_Space ).get.set( false )
            }
            keyMap get Character.toLowerCase( p1.getKeyChar ) collect { case svar => svar.set(false) }


          }

          override def keyPressed(p1: KeyEvent) {
            if( p1.getKeyCode == 37 ) {
              kb.get(  siris.ontology.types.Key_Left ).get.set( true )
            } else if( p1.getKeyCode == 39 ) {
              kb.get(  siris.ontology.types.Key_Right ).get.set( true )
            } else if( p1.getKeyCode == 38 ) {
              kb.get(  siris.ontology.types.Key_Up ).get.set( true )
            } else if( p1.getKeyCode == 40 ) {
              kb.get(  siris.ontology.types.Key_Down ).get.set( true )
            } else if( p1.getKeyCode == 32 ) {
              kb.get(  siris.ontology.types.Key_Space ).get.set( true )
            }
          }
        })

        keyboardEntity = kb
        WorldInterface.registerEntity( 'keyboard :: Symbol( id.toString ) :: Symbol( winId.toString ) :: Nil, kb )
        debug( "Registring window listener" )
        window.addWindowListener( new WindowListener {
          override def windowClose(p1: RenderWindow) {
            config.sender ! JVRRenderWindowClosed( me )
            me.shutdown()
          }

          override def windowReshape(p1: RenderWindow, p2: Int, p3: Int) {}
        })

        mouseEntity = new Entity

        mouseEntity = mouseEntity.injectSVar( SVarImpl( Vec2f( 0, 0)  ), siris.ontology.types.Position2D)
        mouseEntity = mouseEntity.injectSVar( SVarImpl( false  ), siris.ontology.types.Button_Left)
        mouseEntity = mouseEntity.injectSVar( SVarImpl( false  ), siris.ontology.types.Button_Right)
        mouseEntity = mouseEntity.injectSVar( SVarImpl( false  ), siris.ontology.types.Button_Center)
        mouseEntity = mouseEntity.injectSVar( SVarImpl( Vec3(0,0,0)), siris.ontology.types.Origin )
        mouseEntity = mouseEntity.injectSVar( SVarImpl( Vec3(0,0,0)), siris.ontology.types.Direction )


        WorldInterface.registerEntity( 'mouse :: Symbol( id.toString ) :: Symbol( winId.toString ) :: Nil, mouseEntity )

        window.addMouseListener( new MouseListener {
          def mouseWheelMoved(e: MouseEvent) {
            //println( e.getWheelRotation )
          }

          def mouseReleased(e: MouseEvent) {

            if( e.getButton == 1 ) {
              mouseEntity.get(  siris.ontology.types.Button_Left ).get.set( false )
            } else if( e.getButton == 2 ) {
              mouseEntity.get(  siris.ontology.types.Button_Center ).get.set( false )
            } else if( e.getButton == 3 ) {
              mouseEntity.get(  siris.ontology.types.Button_Right ).get.set( false )
            }
            updatePickRay(e)
          }

          def mousePressed(e: MouseEvent) {
            if( e.getButton == 1 ) {
              mouseEntity.get(  siris.ontology.types.Button_Left ).get.set( true )
            } else if( e.getButton == 2 ) {
              mouseEntity.get(  siris.ontology.types.Button_Center ).get.set( true )
            } else if( e.getButton == 3 ) {
              mouseEntity.get(  siris.ontology.types.Button_Right ).get.set( true )
            }
            updatePickRay(e)
          }

          def mouseMoved( e: MouseEvent ) {
            updatePickRay(e)
            mouseEntity.get( siris.ontology.types.Position2D ).get.set( Vec2f( e.getX.toFloat, e.getY.toFloat ) )
          }

          def mouseExited(e: MouseEvent) {}

          def mouseEntered(e: MouseEvent) {}

          def mouseDragged(e: MouseEvent) {
            updatePickRay(e)
            mouseEntity.get( siris.ontology.types.Position2D ).get.set( Vec2f( e.getX.toFloat, e.getY.toFloat ) )
          }

          def mouseClicked(e: MouseEvent) {
            updatePickRay(e)
          }

          private def updatePickRay(e: MouseEvent) {
            val pickRay = Picker.getPickRay(sceneRoot, cameras.head._2.head._2, e.getNormalizedX, e.getNormalizedY)
            mouseEntity.get(siris.ontology.types.Origin).get.set(ConstVec3(pickRay.getRayOrigin.x, pickRay.getRayOrigin.y, pickRay.getRayOrigin.z))
            mouseEntity.get(siris.ontology.types.Direction).get.set(ConstVec3(pickRay.getRayDirection.x, pickRay.getRayDirection.y, pickRay.getRayDirection.z))
          }
        })

        windows = windows ::: window :: Nil
        winId += 1
      }
      viewer = new Viewer( false , windows : _* )
      sender ! JVRRenderActorConfigCompleted( Actor.self )

  }

  addHandler[SetAmbientColor]{
    case msg =>
      for((window, ptr) <- ambientUniforms) {
        ptr.setUniform("jvr_Material_Ambient", new UniformColor(msg.color))
      }
  }

  addHandler[RenderNextFrame] {
    case RenderNextFrame( sender ) =>
      //trace( "Rendering next frame" )
      if( autoRender ) startTimeMeasurement()

      for( updater <- cameraUpdater ) updater.update()
      for( (name,waterMat) <- waterMats ) waterMat.setUniform( name + "_pass", "waveTime", new UniformFloat( (System.nanoTime -t0) * (1e-9).asInstanceOf[Float] ))
      for( ( camera, s ) <- skyboxes ) {
        s.setTransform( camera.getEyeWorldTransform( sceneRoot ).extractTranslation )
      }

      if( !paused ) {
        for( r <- renderStepObserver ) r ! StartingFrame( Actor.self )

        if( viewer == null ) println("Viewer ist null!" )
        val deltaT = (System.nanoTime - this.lastFrame) / 1000000000.0f
        for( ppe <- this.postProcessingEffects ) ppe.setCurrentDeltaT( deltaT )
        viewer.display()
        if( autoRender ) {
          //Actor.self ! RenderNextFrame( Actor.self )
          requestWakeUpCall(timeToNextFrame())
        }
        for( r <- renderStepObserver ) r ! FinishedFrame( Actor.self )
        this.lastFrame = System.nanoTime
      }
  }

  addHandler[ToggleAutoRender] {
    case ToggleAutoRender( sender, a ) =>
      autoRender = a
      debug( "Setting auto render to {}", a )
  }

  addHandler[PauseRenderer] {
    case PauseRenderer( sender ) =>
      paused = true
      debug( "Paused" )
  }

  addHandler[ResumeRenderer] {
    case ResumeRenderer( sender ) =>
      paused = false
      if( autoRender ) Actor.self ! RenderNextFrame( Actor.self )
      trace( "Resumed" )
  }

  trace( "Adding Handler for SubscribeForRenderSteps message  " )
  addHandler[SubscribeForRenderSteps] {
    case SubscribeForRenderSteps( sender ) =>
      renderStepObserver = renderStepObserver ::: sender :: Nil
  }

  trace( "Adding Handler for UnsubscribeForRenderSteps message  " )
  addHandler[UnsubscribeForRenderSteps] {
    case UnsubscribeForRenderSteps( sender ) =>
      renderStepObserver = renderStepObserver.filterNot( _ == sender )
  }

  addHandler[CreateMesh]{ msg =>
    createMesh(msg.e, msg.asp, msg.given)
    msg.sender ! MeshCreated(msg.configActor, msg.ready)
  }

  addHandler[PublishSceneElement] {
    case PublishSceneElement( sender, entity, aspect ) =>
      debug("Received new Scene Element" )
      aspect.createParamSet.semantics match {
        case Symbols.spotLight => collectSVars( entity, siris.components.renderer.jvr.types.Transformation, siris.ontology.types.DiffuseColor, siris.ontology.types.SpecularColor, siris.ontology.types.ConstantAttenuation, siris.ontology.types.LinearAttenuation, siris.ontology.types.QuadraticAttenuation, siris.ontology.types.SpotCutOff, siris.ontology.types.SpotExponent, siris.ontology.types.CastShadow, siris.ontology.types.ShadowBias )( createSpotLight( sender, entity, aspect, _ ) )
        case Symbols.pointLight => collectSVars( entity, siris.components.renderer.jvr.types.Transformation, siris.ontology.types.DiffuseColor, siris.ontology.types.SpecularColor, siris.ontology.types.ConstantAttenuation, siris.ontology.types.LinearAttenuation, siris.ontology.types.QuadraticAttenuation )( createPointLight( sender, entity, aspect, _ ) )
        case Symbols.skyBox => createSkyBox( sender, entity, aspect )
        case Symbols.shapeFromFile => collectSVars( entity, siris.components.renderer.jvr.types.Transformation )( createShapeFromFile( sender, entity, aspect, _ ) )
        case Symbols.mirror => collectSVars( entity, siris.components.renderer.jvr.types.Transformation )( createMirror( sender, entity, aspect, _ ) )
        case Symbols.water => collectSVars( entity, siris.components.renderer.jvr.types.Transformation, siris.ontology.types.WaveScale )( createWater( sender, entity, aspect, _ ) )
        case Symbols.existingNode => createExistingNode( sender, entity, aspect )
        case Symbols.fog => createFog( sender, entity, aspect )
        case Symbols.saturationEffect => collectSVars( entity, siris.ontology.types.Saturation )( createSaturationEffect( sender, entity, aspect, _ ) )
        case Symbols.bloomEffect =>  collectSVars( entity, siris.ontology.types.Threshold, siris.ontology.types.Factor )( createBloomEffect( sender, entity, aspect, _ ) )
        case Symbols.depthOfFieldEffect => collectSVars( entity, siris.ontology.types.Intensity )( createDepthOfFieldEffect( sender, entity, aspect, _ ) )
        case Symbols.interface => createInterface( sender, entity, aspect )
        case Symbols.postProcessingEffect => createPostProcessingEffect( sender, entity, aspect )
        case Symbols.mesh => insertMesh(sender, entity, aspect)
        case _ => error( "Unknown create parameter." )
      }
  }

  addHandler[RemoveSceneElement] {

    case RemoveSceneElement( sender, entity ) =>

      entityToNodeMap( entity ).getParentNode.asInstanceOf[GroupNode].removeChildNode( entityToNodeMap( entity ) )

      entity.get( gt.Transformation ).get.ignore()
      entity.get( siris.ontology.types.LinearAttenuation ) match {
        case Some( sVar ) => sVar.ignore()
        case None =>
      }


  }

  /**
   * This constrcuts the pipeline for the render window.
   *
   * @param pipeline The pipeline that should be filles.
   * @param cameras A map of the cameras that should be used.
   * @param sceneRoot The root of the screne graph.
   */
  def createDefaultPipeline( pipeline: Pipeline, cameras: Map[Symbol,VRCameraNode], sceneRoot: GroupNode, window : RenderWindow ) = {

    //pipeline.setViewFrustumCullingMode( 2 )
    // TODO: Remove this hack for SimThief Content or wrap it in some useful way.
    ambientUniforms = ambientUniforms.updated(window,
      pipeline.setUniform("jvr_Material_Ambient", new UniformColor(new Color(0.3f, 0.3f, 0.3f, 1f)))
    )

    if( shadows ) {
      if( shadowQuality.equals( "low" ) ) {
        pipeline.createFrameBufferObject("ShadowMap", true, 0, 512, 512, 0)
      } else if( shadowQuality.equals( "middle" ) ) {
        pipeline.createFrameBufferObject("ShadowMap", true, 0, 1024, 1024, 0)
      } else if( shadowQuality.equals( "high" ) ) {
        pipeline.createFrameBufferObject("ShadowMap", true, 0, 2048, 2048, 0)
      }
    }

    //pipeline.setUniform("jvr_Material_Ambient", new UniformColor(new Color(0.3f, 0.3f, 0.3f, 1f)))
    for( fbo <- effectplanes ) {
      if( mirrorQuality.equals( "low" ))
        pipeline.createFrameBufferObject( fbo, false, 1, 0.5f, 0 )
      else if( mirrorQuality.equals( "middle" ))
        pipeline.createFrameBufferObject( fbo, false, 1, 0.75f, 0 )
      else if( mirrorQuality.equals( "high" ))
        pipeline.createFrameBufferObject( fbo, false, 1, 1.0f, 0 )
    }

    pipeline.createFrameBufferObject( "siris_fbo_a", true, 1, 1.0f, 4 )
    pipeline.createFrameBufferObject( "siris_fbo_b", true, 1, 1.0f, 4 )
    pipeline.createFrameBufferObject( "siris_fbo_c", true, 1, 1.0f, 4 )

    if( cameras.contains( 'standard ) ) {
      pipeline.setUniform("jvr_UseClipPlane0", new UniformBool(true))
      for( c <- effectplanes ) {
        simplePipeline( pipeline, mirrorCameras(window)( cameras( 'standard ) ), sceneRoot, Symbol( c ), c )
        if( shadows ) {
          pipeline.switchFrameBufferObject("ShadowMap")
          pipeline.clearBuffers(true, true, null)
        }
      }
      pipeline.setUniform("jvr_UseClipPlane0", new UniformBool(true))
      simplePipeline( pipeline, cameras, sceneRoot, 'standard, "siris_fbo_a" )
      pipeline.switchCamera( cameras( 'standard ) )
      for( c <- effectplanes ) {
        pipeline.bindColorBuffer("jvr_MirrorTexture", c, 0 )
        pipeline.drawGeometry(c + "_pass", null)
      }


      var fboB = true

      pipeline.switchFrameBufferObject( "siris_fbo_b" )
      pipeline.clearBuffers(true, true, new Color(125, 125, 125))

      pipeline.switchFrameBufferObject( "siris_fbo_c" )
      pipeline.unbindBuffers()
      val printMaterial = new ShaderMaterial( "PRINT", new ShaderProgram( new File( "pipeline_shader/quad.vs" ), new File( "pipeline_shader/default.fs" ) ) )
      pipeline.clearBuffers(true, true, new Color(125, 125, 125))
      pipeline.bindColorBuffer("jvr_Texture0", "siris_fbo_a", 0)
      pipeline.drawQuad(printMaterial, "PRINT")


      var switchFBO = false
      for( ppe <- this.postProcessingEffects ) {
        if( ppe.isOverlay ) {
          ppe.contributeToPipeline( pipeline, if( fboB ) "siris_fbo_c" else "siris_fbo_b", "siris_fbo_a" )
        } else {
          if( switchFBO ) fboB = !fboB
          pipeline.switchFrameBufferObject( if( fboB ) "siris_fbo_b" else "siris_fbo_c" )
          switchFBO = ppe.contributeToPipeline( pipeline, if( fboB ) "siris_fbo_c" else "siris_fbo_b", "siris_fbo_a" )
        }

      }
      if( switchFBO ) fboB = !fboB

      pipeline.switchFrameBufferObject( null )
      pipeline.unbindBuffers()
      pipeline.clearBuffers(true, true, new Color(125, 125, 125))
      pipeline.bindColorBuffer("jvr_Texture0", if( fboB ) "siris_fbo_c" else "siris_fbo_b", 0)
      pipeline.drawQuad(printMaterial, "PRINT")


    } else {
      println( "creating eye maps" )
      pipeline.createFrameBufferObject("LeftEyeMap", true, 1, 1.0f, 4)
      pipeline.createFrameBufferObject("RightEyeMap", true, 1, 1.0f, 4)

      pipeline.setUniform("jvr_UseClipPlane0", new UniformBool(true))
      for( c <- effectplanes ) {
        simplePipeline( pipeline, mirrorCameras(window)( cameras( 'standardLeft ) ), sceneRoot, Symbol( c ), c )
        if( shadows ) {
          pipeline.switchFrameBufferObject("ShadowMap")
          pipeline.clearBuffers(true, true, null)
        }
      }

      pipeline.setUniform("jvr_UseClipPlane0", new UniformBool(true))
      pipeline.switchFrameBufferObject( "LeftEyeMap" )
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))

      simplePipeline( pipeline, cameras, sceneRoot, 'standardLeft, "LeftEyeMap" )
      for( c <- effectplanes ) {
        pipeline.bindColorBuffer("jvr_MirrorTexture", c, 0 )
        pipeline.drawGeometry(c + "_pass", null)
      }

      var fboB = true

      pipeline.switchFrameBufferObject( "siris_fbo_c" )
      pipeline.unbindBuffers()
      val printMaterial = new ShaderMaterial( "PRINT", new ShaderProgram( new File( "pipeline_shader/quad.vs" ), new File( "pipeline_shader/default.fs" ) ) )
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))
      pipeline.bindColorBuffer("jvr_Texture0", "LeftEyeMap", 0)
      pipeline.drawQuad(printMaterial, "PRINT")

      var switchFBO = false
      for( ppe <- this.postProcessingEffects ) {
        if( ppe.isOverlay ) {
          ppe.contributeToPipeline( pipeline, if( fboB ) "siris_fbo_c" else "siris_fbo_b", "siris_fbo_a" )
        } else {
          if( switchFBO ) fboB = !fboB
          pipeline.switchFrameBufferObject( if( fboB ) "siris_fbo_b" else "siris_fbo_c" )
          switchFBO = ppe.contributeToPipeline( pipeline, if( fboB ) "siris_fbo_c" else "siris_fbo_b", "siris_fbo_a" )
        }

      }
      if( switchFBO ) fboB = !fboB

      pipeline.switchFrameBufferObject( "LeftEyeMap" )
      pipeline.unbindBuffers()
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))
      pipeline.bindColorBuffer("jvr_Texture0", if( fboB ) "siris_fbo_c" else "siris_fbo_b", 0)
      pipeline.drawQuad(printMaterial, "PRINT")

      pipeline.unbindBuffers()
      pipeline.setUniform("jvr_UseClipPlane0", new UniformBool(true))
      for( c <- effectplanes ) {
        simplePipeline( pipeline, mirrorCameras(window)( cameras('standardRight) ), sceneRoot, Symbol( c ), c )
        if( shadows ) {
          pipeline.switchFrameBufferObject("ShadowMap")
          pipeline.clearBuffers(true, true, null)
        }
      }

      pipeline.setUniform("jvr_UseClipPlane0", new UniformBool(true))
      pipeline.switchFrameBufferObject( "RightEyeMap" )
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))


      simplePipeline( pipeline, cameras, sceneRoot, 'standardRight, "RightEyeMap" )
      for( c <- effectplanes ) {
        pipeline.bindColorBuffer("jvr_MirrorTexture", c, 0 )
        pipeline.drawGeometry(c + "_pass", null)
      }

      fboB = true

      pipeline.switchFrameBufferObject( "siris_fbo_c" )
      pipeline.unbindBuffers()
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))
      pipeline.bindColorBuffer("jvr_Texture0", "RightEyeMap", 0)
      pipeline.drawQuad(printMaterial, "PRINT")


      switchFBO = false
      for( ppe <- this.postProcessingEffects ) {
        if( ppe.isOverlay ) {
          ppe.contributeToPipeline( pipeline, if( fboB ) "siris_fbo_c" else "siris_fbo_b", "siris_fbo_a" )
        } else {
          if( switchFBO ) fboB = !fboB
          pipeline.switchFrameBufferObject( if( fboB ) "siris_fbo_b" else "siris_fbo_c" )
          switchFBO = ppe.contributeToPipeline( pipeline, if( fboB ) "siris_fbo_c" else "siris_fbo_b", "siris_fbo_a" )
        }

      }
      if( switchFBO ) fboB = !fboB

      pipeline.switchFrameBufferObject( "RightEyeMap" )
      pipeline.unbindBuffers()
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))
      pipeline.bindColorBuffer("jvr_Texture0", if( fboB ) "siris_fbo_c" else "siris_fbo_b", 0)
      pipeline.drawQuad(printMaterial, "PRINT")

    }
    if( stereoType.isDefined && stereoType.get == StereoType.AnaglyphStereo ) {
      val anaShader =
        new ShaderProgram(
          new File("pipeline_shader/quad.vs"),
          new File("pipeline_shader/ana.fs")
        )
      val anaMaterial = new ShaderMaterial("ANAGLYPH", anaShader)

      pipeline.switchFrameBufferObject(null)
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))


      pipeline.bindColorBuffer("leftEye", "LeftEyeMap", 0)
      pipeline.bindColorBuffer("rightEye", "RightEyeMap", 0)
      pipeline.drawQuad(anaMaterial, "ANAGLYPH")

    } else if( stereoType.isDefined && stereoType.get == StereoType.FrameSequential ) {

      pipeline.switchFrameBufferObject( null )
      pipeline.unbindBuffers()
      pipeline.setDrawBuffer( GL2GL3.GL_BACK_LEFT )
      val printMaterial = new ShaderMaterial( "PRINT", new ShaderProgram( new File( "pipeline_shader/quad.vs" ), new File( "pipeline_shader/default.fs" ) ) )
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))
      pipeline.bindColorBuffer("jvr_Texture0", "LeftEyeMap", 0)
      pipeline.drawQuad(printMaterial, "PRINT")

      pipeline.unbindBuffers()

      pipeline.setDrawBuffer( GL2GL3.GL_BACK_RIGHT )
      pipeline.clearBuffers(true, true, new Color(0, 0, 0))
      pipeline.bindColorBuffer("jvr_Texture0", "RightEyeMap", 0)
      pipeline.drawQuad(printMaterial, "PRINT")


    }




  }

  /**
   * This function creates a simple pipeline with shadows.
   *
   * @param pipeline The pipeline that should be filles.
   * @param cameras A map of the cameras that should be used.
   * @param sceneRoot The root of the screne graph.
   * @param cam A symbol to the cameras that should be used.
   * @param target The name of the target frame buffer.
   */
  def simplePipeline( pipeline: Pipeline, cameras: Map[Symbol,VRCameraNode], sceneRoot: GroupNode, cam : Symbol, target: String ) = {

    pipeline.switchCamera( cameras( cam ) )
    pipeline.switchFrameBufferObject(target)
    pipeline.clearBuffers(true, true, new Color(.5f,.5f,.5f) )
    ///
    //    pipeline.setDepthTest( false )
    //    pipeline.setBackFaceCulling( false )
    //    pipeline.drawGeometry( cameras( cam ).getName, null)
    ///
    pipeline.setDepthTest( true )
    pipeline.drawGeometry("AMBIENT", null)


    if( shadows ) {
      val lp = pipeline.doLightLoop(false, true)

      lp.switchLightCamera()
      lp.switchFrameBufferObject("ShadowMap")
      lp.clearBuffers(true, false, null)
      lp.drawGeometry("AMBIENT", null)
      lp.switchFrameBufferObject( target )
      lp.switchCamera( cameras( cam ) )
      lp.bindDepthBuffer("jvr_ShadowMap", "ShadowMap" )
      lp.drawGeometry("LIGHTING", null)

      //      pipeline.doLightLoop(true, false).drawGeometry( "LIGHTING", null )     ///
    } else {
      pipeline.doLightLoop(true, true).drawGeometry( "LIGHTING", null )
    }

  }

  private def createSpotLight( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList ) {
    val name = aspect.createParamSet.firstValueFor( siris.ontology.types.Name )
    val transformation = fresh.firstValueFor( siris.components.renderer.jvr.types.Transformation )
    val parentElement = aspect.createParamSet.getFirstValueFor( siris.ontology.types.ParentElement )
    val diffuseColor = fresh.firstValueFor( siris.components.renderer.jvr.types.DiffuseColor )
    val specularColor = fresh.firstValueFor(  siris.components.renderer.jvr.types.SpecularColor )
    val constantAttenuation = fresh.firstValueFor( siris.ontology.types.ConstantAttenuation )
    val linearAttenuation = fresh.firstValueFor( siris.ontology.types.LinearAttenuation )
    val quadraticAttenuation = fresh.firstValueFor(  siris.ontology.types.QuadraticAttenuation )
    val spotCutOff = fresh.firstValueFor( siris.ontology.types.SpotCutOff )
    val spotExponent = fresh.firstValueFor( siris.ontology.types.SpotExponent )
    val castShadow = fresh.firstValueFor( siris.ontology.types.CastShadow )
    val shadowBias = fresh.firstValueFor( siris.ontology.types.ShadowBias )

    val spotLight = new SpotLightNode( name )
    spotLight.setTransform( transformation )
    spotLight.setDiffuseColor( diffuseColor )
    spotLight.setSpecularColor( specularColor )
    spotLight.setConstantAttenuation( constantAttenuation )
    spotLight.setLinearAttenuation( linearAttenuation )
    spotLight.setQuadraticAttenuation( quadraticAttenuation )
    spotLight.setSpotCutOff( spotCutOff )
    spotLight.setSpotExponent( spotExponent )
    spotLight.setCastShadow( castShadow )
    spotLight.setShadowBias( shadowBias )

    val hullNode = new GroupNode( spotLight.getName + "_hull")
    hullNode.setTransform( transformation )
    hullNode.addChildNode( spotLight )
    spotLight.setTransform( new Transform )
    entityToNodeMap = entityToNodeMap + (entity -> hullNode )


    parentElement match {
      case Some( ent ) => entityToNodeMap( ent ).addChildNode( hullNode )
      case None => sceneRoot.addChildNode( hullNode )
    }

    val transformationSVar = entity.get( siris.components.renderer.jvr.types.Transformation ).get
    addSVarUpdateFunctions(
      transformationSVar,
      hullNode.setTransform( _ : Transform ),
      hullNode.getTransform
    )

    val diffuseColorSVar = entity.get( siris.components.renderer.jvr.types.DiffuseColor ).get
    addSVarUpdateFunctions(
      diffuseColorSVar,
      spotLight.setDiffuseColor( _ : Color ),
      spotLight.getDiffuseColor
    )

    val specularColorSVar = entity.get( siris.components.renderer.jvr.types.SpecularColor ).get
    addSVarUpdateFunctions(
      specularColorSVar,
      spotLight.setSpecularColor( _ : Color ),
      spotLight.getSpecularColor
    )

    val constantAttenuationSVar = entity.get( siris.ontology.types.ConstantAttenuation ).get
    addSVarUpdateFunctions(
      constantAttenuationSVar,
      spotLight.setConstantAttenuation( _ : Float ),
      spotLight.getConstantAttenuation
    )

    val linearAttenutationSVar = entity.get( siris.ontology.types.LinearAttenuation ).get
    addSVarUpdateFunctions(
      linearAttenutationSVar,
      spotLight.setLinearAttenuation( _ : Float ),
      spotLight.getLinearAttenuation
    )

    val quadraticAttenutationSVar = entity.get( siris.ontology.types.QuadraticAttenuation ).get
    addSVarUpdateFunctions(
      quadraticAttenutationSVar,
      spotLight.setQuadraticAttenuation( _ : Float ),
      spotLight.getQuadraticAttenuation
    )

    val spotCutOffSVar = entity.get( siris.ontology.types.SpotCutOff ).get
    addSVarUpdateFunctions(
      spotCutOffSVar,
      spotLight.setSpotCutOff( _ : Float ),
      spotLight.getSpotCutOff
    )

    val spotExponentSVar = entity.get( siris.ontology.types.SpotExponent ).get
    addSVarUpdateFunctions(
      spotExponentSVar,
      spotLight.setSpotExponent( _ : Float ),
      spotLight.getSpotExponent
    )

    val castShadowSVar = entity.get( siris.ontology.types.CastShadow ).get
    addSVarUpdateFunctions(
      castShadowSVar,
      spotLight.setCastShadow( _ : Boolean ),
      spotLight.isCastingShadow
    )

    val shadowBiasSVar = entity.get( siris.ontology.types.ShadowBias ).get
    addSVarUpdateFunctions(
      shadowBiasSVar,
      spotLight.setShadowBias( _ : Float ),
      spotLight.getShadowBias
    )
    sender ! ElementInjected( Actor.self, entity )
  }

  private def createPointLight( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList ) {

    val name = aspect.createParamSet.firstValueFor( siris.ontology.types.Name )
    val transformation = fresh.firstValueFor( siris.components.renderer.jvr.types.Transformation )
    val parentElement = aspect.createParamSet.getFirstValueFor( siris.ontology.types.ParentElement )
    val diffuseColor = fresh.firstValueFor( siris.components.renderer.jvr.types.DiffuseColor )
    val specularColor = fresh.firstValueFor( siris.components.renderer.jvr.types.SpecularColor )
    val constantAttenuation = fresh.firstValueFor( siris.ontology.types.ConstantAttenuation )
    val linearAttenuation = fresh.firstValueFor( siris.ontology.types.LinearAttenuation )
    val quadraticAttenuation = fresh.firstValueFor(  siris.ontology.types.QuadraticAttenuation )

    val pointLight = new PointLightNode( name )
    pointLight.setTransform( transformation )
    pointLight.setDiffuseColor( diffuseColor )
    pointLight.setSpecularColor( specularColor )
    pointLight.setConstantAttenuation( constantAttenuation )
    pointLight.setLinearAttenuation( linearAttenuation )
    pointLight.setQuadraticAttenuation( quadraticAttenuation )

    val hullNode = new GroupNode( pointLight.getName + "_hull")
    hullNode.setTransform( transformation )
    hullNode.addChildNode( pointLight )
    pointLight.setTransform( new Transform )
    entityToNodeMap = entityToNodeMap + (entity -> hullNode )


    parentElement match {
      case Some( ent ) => entityToNodeMap( ent ).addChildNode( hullNode )
      case None => sceneRoot.addChildNode( hullNode )
    }

    val transformationSVar = entity.get( siris.components.renderer.jvr.types.Transformation ).get
    addSVarUpdateFunctions(
      transformationSVar,
      hullNode.setTransform( _ : Transform ),
      hullNode.getTransform
    )

    val diffuseColorSVar = entity.get( siris.components.renderer.jvr.types.DiffuseColor ).get
    addSVarUpdateFunctions(
      diffuseColorSVar,
      pointLight.setDiffuseColor( _ : Color ),
      pointLight.getDiffuseColor
    )

    val specularColorSVar = entity.get( siris.components.renderer.jvr.types.SpecularColor ).get
    addSVarUpdateFunctions(
      specularColorSVar,
      pointLight.setSpecularColor( _ : Color ),
      pointLight.getSpecularColor
    )

    val constantAttenuationSVar = entity.get( siris.ontology.types.ConstantAttenuation ).get
    addSVarUpdateFunctions(
      constantAttenuationSVar,
      pointLight.setConstantAttenuation( _ : Float ),
      pointLight.getConstantAttenuation
    )

    val linearAttenutationSVar = entity.get( siris.ontology.types.LinearAttenuation ).get
    addSVarUpdateFunctions(
      linearAttenutationSVar,
      pointLight.setLinearAttenuation( _ : Float ),
      pointLight.getLinearAttenuation
    )

    val quadraticAttenutationSVar = entity.get( siris.ontology.types.QuadraticAttenuation ).get
    addSVarUpdateFunctions(
      quadraticAttenutationSVar,
      pointLight.setQuadraticAttenuation( _ : Float ),
      pointLight.getQuadraticAttenuation
    )
    sender ! ElementInjected( Actor.self, entity )
  }


  private def createSkyBox( sender: Actor, entity : Entity with Removability, aspect : EntityAspect ) {
    val name = aspect.createParamSet.firstValueFor( siris.ontology.types.Name  )
    val backTexture = aspect.createParamSet.firstValueFor( siris.ontology.types.BackTexture  )
    val bottomTexture = aspect.createParamSet.firstValueFor( siris.ontology.types.DownTexture )
    val frontTexture = aspect.createParamSet.firstValueFor( siris.ontology.types.FrontTexture )
    val leftTexture = aspect.createParamSet.firstValueFor( siris.ontology.types.LeftTexture )
    val rightTexture = aspect.createParamSet.firstValueFor( siris.ontology.types.RightTexture )
    val topTexture = aspect.createParamSet.firstValueFor( siris.ontology.types.UpTexture )

    val creater = SkyBoxCreater(
      name = name,
      backTexture = backTexture,
      bottomTexture = bottomTexture,
      frontTexture = frontTexture,
      leftTexture = leftTexture,
      rightTexture = rightTexture,
      topTexture = topTexture
    )
    skyBoxCreater = Some(creater)

    for( (_,c ) <- cameras ) {
      for( (_,camera) <- c ) {
        val skyBox = creater.create( camera.getName )
        sceneRoot.addChildNode( skyBox )
        skyboxes = skyboxes + (camera -> skyBox)
      }
    }

    for( w <- windows ) {
      for( (_,mc ) <- mirrorCameras(w) ) {
        for( (_,camera) <- mc ) {
          val skyBox = creater.create( camera.getName )
          sceneRoot.addChildNode( skyBox )
          skyboxes = skyboxes + (camera -> skyBox)
        }
      }
    }
    sender ! ElementInjected( Actor.self, entity )
  }

  private def asMesh(in : Any) : ColoredMesh = in match{
    case geom : ColoredMesh => geom
    case something => throw new Exception("Error " + something.asInstanceOf[AnyRef].getClass.getCanonicalName +
      " is no instance of de.bht.jvr.core.Geometry")
  }

  def makeMeshMaterial(tex : Option[Texture2D]) : ShaderMaterial = {
    val sm = new ShaderMaterial
    val avs = new FileInputStream(new java.io.File("shader/myShader.vs"))
    val afs = new FileInputStream(new java.io.File("shader/myShader.fs"))
    val amb = new ShaderProgram(new Shader(avs, GL2ES2.GL_VERTEX_SHADER), new Shader(afs, GL2ES2.GL_FRAGMENT_SHADER))

    val cl = ClassLoader.getSystemClassLoader
    val dvs = cl.getResourceAsStream("de/bht/jvr/shaders/phong_lighting.vs")
    val dfs = cl.getResourceAsStream("de/bht/jvr/shaders/phong_lighting.fs")
    val dmb = new ShaderProgram(new Shader(dvs, GL2ES2.GL_VERTEX_SHADER), new Shader(dfs, GL2ES2.GL_FRAGMENT_SHADER))

    sm.setShaderProgram("AMBIENT", amb)
    sm.setUniform("AMBIENT",  "jvr_Material_Ambient",   new UniformColor(new Color(0.3f, 0.3f, 0.3f, 1.0f)))

    sm.setShaderProgram("LIGHTING", dmb)
    sm.setUniform("LIGHTING", "jvr_Material_Diffuse",   new UniformColor(new Color(0.5f, 0.5f, 0.5f, 1.0f)))
    sm.setUniform("LIGHTING", "jvr_Material_Specular",  new UniformColor(new Color(0.5f, 0.5f, 0.5f, 1.0f)))
    sm.setUniform("LIGHTING", "jvr_Material_Shininess", new UniformFloat(10))

    tex match {
      case Some(texture) =>
        sm.setTexture("AMBIENT",  "jvr_Texture0", texture)
        sm.setTexture("LIGHTING", "jvr_Texture0", texture)
        sm.setUniform("LIGHTING", "jvr_UseTexture0", new UniformBool(true))
        sm.setUniform("AMBIENT",  "jvr_UseTexture0", new UniformBool(true))
      case None =>
        sm.setUniform("AMBIENT",  "jvr_UseTexture0", new UniformBool(false))
        sm.setUniform("LIGHTING", "jvr_UseTexture0", new UniformBool(false))
    }

    sm
  }

  private def createMesh(entity : Entity, aspect : EntityAspect, given : SValList){
    val createParams = aspect.createParamSet
    //
    val texture              = given.getFirstValueFor( types.Texture )
    val scale                = aspect.createParamSet.firstValueFor( siris.components.renderer.jvr.types.Scale )
    val transformation       = createParams.firstValueFor( siris.components.renderer.jvr.types.Transformation )
    val material             = makeMeshMaterial( texture )
    val geometry             = asMesh( given.firstValueFor( siris.ontology.types.Mesh ) )
    //
    val shape     = new ShapeNode("mesh_" + entity.id, geometry, material)
    val scaleNode = new GroupNode( shape.getName + "_scale" )
    val hullNode  = new GroupNode( shape.getName + "_hull" )
    //

    shape.setTransform( new Transform )
    hullNode.setTransform( transformation )
    scaleNode.setTransform( scale )

    hullNode.addChildNode( scaleNode )
    scaleNode.addChildNode( shape )

    texture.collect{ case t2d => entityToTexMap = entityToTexMap + (entity -> t2d) }
    entityToGeoMap      = entityToGeoMap      + (entity -> geometry)
    entityToNodeMap     = entityToNodeMap     + (entity -> hullNode)
  }

  private def insertMesh( sender: Actor, entity : Entity with Removability, aspect : EntityAspect)  {
    val createParams = aspect.createParamSet
    val parentElement  = createParams.getFirstValueFor( siris.ontology.types.ParentElement )
    val hullNode = entityToNodeMap.get(entity).get
    parentElement match {
      case Some( e ) => entityToNodeMap( entity ).addChildNode( hullNode )
      case None => sceneRoot.addChildNode( hullNode )
    }

    //update mesh
    //TODO: check if the node has been removed
    getShapeChild(hullNode).collect {
      case shapeNode =>
        // mesh update
        entity.get(gt.Mesh).collect   {
          case sVar => sVar.observe{
            mesh =>
              val geo = entityToGeoMap.get(entity).get
              val msh = asMesh(mesh)
              geo.setNormals(msh.getNormals.clone())
              geo.setTexCoords(msh.getTexCoords.clone())
              geo.setVertices(msh.getIndices, msh.getIndicesCount, msh.getVertices)
              shapeNode.setGeometry(geo)
          }
        }
        // tex update
        entity.get(types.Texture).collect{
          case sVar => sVar.observe{
            tex => entityToTexMap.get(entity) match {
              case Some(t) =>
                t.updateTexture(tex.getWidth, tex.getHeight, tex.getNumChannels, tex.getImageBuffer)
                t.setFormat(tex.getFormat)
              case None =>
                shapeNode.setMaterial(makeMeshMaterial(Some(tex)))
            }
          }
        }
    }

    addSVarUpdateFunctions(
      entity.get( siris.components.renderer.jvr.types.Transformation ).get,
      ( ( t: Transform ) => hullNode.setTransform( t )),
      hullNode.getTransform
    )
    sender ! ElementInjected( Actor.self, entity )
  }

  private def getShapeChild( parent : GroupNode) : Option[ShapeNode] = {
    val iterator = parent.getChildNodes.iterator()
    while (iterator.hasNext){
      val found = parent.getChildNodes.iterator().next() match {
        case shapeNode : ShapeNode => Some(shapeNode)
        case groupNode : GroupNode => getShapeChild(groupNode)
        case _ => None
      }
      if (found.isDefined)
        return found
    }
    None
  }

  private def createShapeFromFile( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList )  {
    val fileName = aspect.createParamSet.firstValueFor( siris.ontology.types.ColladaFile )
    val subElement = aspect.createParamSet.getFirstValueFor( siris.ontology.types.SubElement )
    val parentElement = aspect.createParamSet.getFirstValueFor( siris.ontology.types.ParentElement )
    val transformation = fresh.firstValueFor( siris.components.renderer.jvr.types.Transformation )
    val scale = aspect.createParamSet.firstValueFor( siris.components.renderer.jvr.types.Scale )
    val manipulators = aspect.createParamSet.getFirstValueFor( siris.ontology.types.ManipulatorList )



    val source = ResourceManager.getSceneNodeFromColladaFile( new File( fileName ) )
    val element = subElement match {
      case Some( name ) => Finder.find( source, classOf[GroupNode], name )
      case None => source
    }


    if( manipulators.isDefined ) {
      // TODO Convert other manipulators to shader program description
      for( m <- manipulators.get.asInstanceOf[List[ElementManipulator]] ) m.execute( element )
    }

    val shape = element
    val scaleNode = new GroupNode( shape.getName + "_scale" )
    val hullNode = new GroupNode( shape.getName + "_hull" )

    hullNode.setTransform( transformation )
    scaleNode.setTransform( scale  )
    shape.setTransform( new Transform )
    hullNode.addChildNode( scaleNode )
    scaleNode.addChildNode( shape )
    entityToNodeMap = entityToNodeMap + (entity -> hullNode )
    parentElement match {
      case Some( e ) => entityToNodeMap( entity ).addChildNode( hullNode )
      case None => sceneRoot.addChildNode( hullNode )
    }

    val transformationSVar = entity.get( siris.components.renderer.jvr.types.Transformation ).get
    addSVarUpdateFunctions(
      transformationSVar,
      ( ( t: Transform ) => hullNode.setTransform( t )),
      hullNode.getTransform
    )
    sender ! ElementInjected( Actor.self, entity )
  }

  //private def createAnimationObject( entity : Entity with Removability, aspect : EntityAspect ) = null

  private def createMirror( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList ) {
    //val transformation = fresh.firstValueFor( siris.components.renderer.jvr.types.Transformation )
    val fileName = aspect.createParamSet.firstValueFor( siris.ontology.types.ColladaFile )
    val name = aspect.createParamSet.firstValueFor( siris.ontology.types.Name )

    val hullNode = new GroupNode( name + "_hull")

    val clipPlane = new ClipPlaneNode
    clipPlane.setTransform( Transform.rotateX( radians( 180f ) ) )
    val shape = ResourceManager.getSceneNodeFromColladaFile( new File( fileName ) )

    val prog = ResourceManager.loadShaderProgram( "shader/mirror.vs" :: "shader/mirror.fs" :: Nil )
    val mirrorMat = new ShaderMaterial( name + "_pass", prog )

    Finder.find(shape, classOf[ShapeNode], null).setMaterial(mirrorMat)

    hullNode.addChildNode( clipPlane )
    hullNode.addChildNode( shape )
    sceneRoot.addChildNode( hullNode )
    entityToNodeMap = entityToNodeMap + (entity -> hullNode )

    for( (window,c) <-camList ) {
      for( cam <- c ) {
        val mirrorCamera = new VRCameraNode( cam.getName + "_mirror_" + name, cam.getScreenTransform, new Vector4( cam.getScreenDim.y, cam.getScreenDim.x, cam.getScreenDim.z, cam.getScreenDim.w ), cam.isLeftEye, cam.getTransform )
        cameraUpdater = cameraUpdater ::: new MirrorCameraUpdater( mirrorCamera, cam, hullNode ) :: Nil
        var map = mirrorCameras(window).get( cam ) match {
          case Some( m ) => m
          case None => Map[Symbol,VRCameraNode]()
        }
        map = map + ( Symbol( name ) -> mirrorCamera)
        mirrorCameras = mirrorCameras + (window -> (mirrorCameras(window) + (cam  -> map)))
        skyBoxCreater match {
          case Some( s ) =>
            val skyBox = s.create( mirrorCamera.getName )
            sceneRoot.addChildNode( skyBox )
            skyboxes = skyboxes + (mirrorCamera -> skyBox)

          case None =>
        }

      }
    }
    effectplanes = effectplanes ::: name :: Nil
    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras(window), sceneRoot, window )
    }

    val transformationSVar = entity.get( siris.components.renderer.jvr.types.Transformation ).get
    addSVarUpdateFunctions(
      transformationSVar,
      ( ( t: Transform ) => hullNode.setTransform( t )),
      hullNode.getTransform
    )
    sender ! ElementInjected( Actor.self, entity )
  }


  /*
  private def createMirror2( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList ) {
    val transformation = fresh.firstValueFor( siris.components.renderer.jvr.types.Transformation )
    val fileName = aspect.createParamSet.firstValueFor( siris.ontology.types.ColladaFile )
    val name = aspect.createParamSet.firstValueFor( siris.ontology.types.Name )

    val hullNode = new GroupNode( name + "_hull")

    val clipPlane = new ClipPlaneNode
    clipPlane.setTransform( Transform.rotateX( radians( 180f ) ) )
    val shape = ResourceManager.getSceneNodeFromColladaFile( new File( fileName ) )

    val prog = ResourceManager.loadShaderProgram( "shader/mirror.vs" :: "shader/mirror.fs" :: Nil )
    val mirrorMat = new ShaderMaterial( name + "_pass", prog )

    Finder.find(shape, classOf[ShapeNode], null).setMaterial(mirrorMat)

    hullNode.addChildNode( clipPlane )
    hullNode.addChildNode( shape )
    sceneRoot.addChildNode( hullNode )
    entityToNodeMap = entityToNodeMap + (entity -> hullNode )

    for( (window,c) <-camList ) {
      for( cam <- c ) {
        val mirrorCamera = new VRCameraNode( cam.getName + "_mirror_" + name, cam.getScreenTransform, new Vector4( cam.getScreenDim.y, cam.getScreenDim.x, cam.getScreenDim.z, cam.getScreenDim.w ), cam.isLeftEye, cam.getTransform )
        cameraUpdater = cameraUpdater ::: new MirrorCameraUpdater( mirrorCamera, cam, hullNode ) :: Nil
        var map = mirrorCameras(window).get( cam ) match {
          case Some( m ) => m
          case None => Map[Symbol,VRCameraNode]()
        }
        map = map + ( Symbol( name ) -> mirrorCamera)
        mirrorCameras = mirrorCameras + (window -> (mirrorCameras(window) + (cam  -> map)))
        //          skyBoxCreater match {
        //            case Some( s ) =>
        //              val skyBox = s.create( mirrorCamera.getName )
        //              sceneRoot.addChildNode( skyBox )
        //              skyboxes = skyboxes + (mirrorCamera -> skyBox)
        //
        //            case None =>
        //          }

      }
    }
    effectplanes = effectplanes ::: name :: Nil
    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras(window), sceneRoot, window )
    }

    val transformationSVar = entity.get( siris.components.renderer.jvr.types.Transformation ).get
    addSVarUpdateFunctions(
      transformationSVar,
      ( ( t: Transform ) => hullNode.setTransform( t )),
      hullNode.getTransform
    )
    sender ! ElementInjected( Actor.self, entity )
  }
  */

  private def createWater( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList ) {
    val transformation = fresh.firstValueFor( siris.components.renderer.jvr.types.Transformation )
    //val fileName = "models/plane.dae"//aspect.createParamSet.firstValueFor( siris.ontology.types.ColladaFile )
    val name = aspect.createParamSet.firstValueFor( siris.ontology.types.Name )
    val waveScaleValue = fresh.firstValueFor( siris.ontology.types.WaveScale )

    val hullNode = new GroupNode( name + "_hull")

    val clipPlane = new ClipPlaneNode
    clipPlane.setTransform( Transform.rotateX( radians( 180f ) ) )
    //val shape = ResourceManager.getSceneNodeFromColladaFile( new File( fileName ) )

    val prog = ResourceManager.loadShaderProgram( "shader/mirror.vs" :: "shader/mirror.fs" :: Nil )
    val waterMat = new ShaderMaterial( name + "_pass", prog )
    waterMat.setUniform( name + "_pass", "waveTime", new UniformFloat( (System.nanoTime -t0) * (1e-9).asInstanceOf[Float] ))
    waterMat.setUniform( name + "_pass","waveScale", new UniformFloat(waveScaleValue))
    waterMats = waterMats + (name -> waterMat)
    //Finder.find(shape, classOf[ShapeNode], null).setMaterial(waterMat);

    //hullNode.addChildNode( clipPlane )
    //hullNode.addChildNode( shape )
    hullNode.setTransform( transformation )
    sceneRoot.addChildNode( hullNode )
    entityToNodeMap = entityToNodeMap + (entity -> hullNode )

    for( (window,c) <- camList ) {
      for( cam <- c ) {
        //val mirrorCamera = new VRCameraNode( cam.getName + "_mirror_" + name, cam.getScreenTransform, new Vector4( cam.getScreenDim.y, cam.getScreenDim.x, cam.getScreenDim.z, cam.getScreenDim.w ), cam.isLeftEye, cam.getTransform )
        val mirrorCamera = new VRCameraNode( cam.getName + "_mirror_" + name, cam.getScreenTransform, new Vector4( cam.getScreenDim.y, cam.getScreenDim.x, cam.getScreenDim.w, cam.getScreenDim.z ), cam.isLeftEye, transformation.mul(cam.getScreenTransform.mul(Transform.scale(-1.0f))) )
        mirrorCamera.setNearPlane(abs(cam.getScreenTransform.getMatrix.translation().z()))
        cameraUpdater = cameraUpdater ::: new MirrorCameraUpdater( mirrorCamera, cam, hullNode ) :: Nil
        var map = mirrorCameras(window).get( cam ) match {
          case Some( m ) => m
          case None => Map[Symbol,VRCameraNode]()
        }
        map = map + ( Symbol( name )  -> mirrorCamera)

        mirrorCameras = mirrorCameras + (window -> (mirrorCameras(window) + (cam  -> map)))

        skyBoxCreater match {
          case Some( s ) =>
            val skyBox = s.create( mirrorCamera.getName )
            sceneRoot.addChildNode( skyBox )
            skyboxes = skyboxes + (mirrorCamera -> skyBox)

          case None =>
        }
      }
    }
    effectplanes = effectplanes ::: name :: Nil

    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras( window ), sceneRoot, window )
    }

    val transformationSVar = entity.get( siris.components.renderer.jvr.types.Transformation ).get
    addSVarUpdateFunctions(
      transformationSVar,
      ( ( t: Transform ) => hullNode.setTransform( t )),
      hullNode.getTransform
    )

    val waveScaleSVar = entity.get( siris.ontology.types.WaveScale ).get
    addSVarUpdateFunctions(
      waveScaleSVar,
      ( ( waveScaleValue: Float ) => waterMat.setUniform( name + "_pass","waveScale", new UniformFloat( waveScaleValue )) ),
      0.0f
    )
    sender ! ElementInjected( Actor.self, entity )
  }

  private def createExistingNode( sender: Actor, entity : Entity with Removability, aspect : EntityAspect ) {
    val subElement = aspect.createParamSet.firstValueFor( siris.ontology.types.SubElement )
    val n : SceneNode = Finder.find( sceneRoot, classOf[SceneNode], subElement )

    val worldTransform = n.getWorldTransform( sceneRoot )
    n.getParentNode.asInstanceOf[GroupNode].removeChildNode( n )

    val hullNode = new GroupNode( n.getName + "_hull" )
    hullNode.addChildNode( n )
    hullNode.setTransform( worldTransform )
    sceneRoot.addChildNode( hullNode )
    entityToNodeMap = entityToNodeMap + (entity -> hullNode )

    val transformationSVar = entity.get(siris.components.renderer.jvr.types.Transformation).get
    addSVarUpdateFunctions(
      transformationSVar,
      ( ( t: Transform ) => hullNode.setTransform( t )),
      hullNode.getTransform
    )
    sender ! ElementInjected( Actor.self, entity )
  }

  private def createFog( sender: Actor, entity : Entity with Removability, aspect : EntityAspect ) {
    val nearClip = aspect.createParamSet.firstValueFor( siris.ontology.types.NearClip )
    val farClip = aspect.createParamSet.firstValueFor( siris.ontology.types.FarClip )
    val skyBoxFog = aspect.createParamSet.firstValueFor(siris.ontology.types.SkyBoxFog)

    val fogPpe = siris.components.renderer.jvr.PostProcessingEffect( "fog" ).
      describedByShaders( "pipeline_shader/quad.vs" :: "pipeline_shader/fog.fs" :: Nil ).
      usingColorBufferAsName( "jvr_Texture1" ).
      usingDepthBufferAsName( "jvr_Texture0" ).
      where( "nearClip" ).hasValue( nearClip ).isReachableBy( gt.NearClip ).
      and( "farClip" ).hasValue( farClip ).isReachableBy( gt.FarClip ).
      and( "skyFog" ).hasValue( skyBoxFog ).isReachableBy( gt.SkyBoxFog ).pack

    this.postProcessingEffects = this.postProcessingEffects ::: fogPpe :: Nil
    this.entityToPostProcessingEffects = this.entityToPostProcessingEffects + (entity -> (fogPpe::Nil))

    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras(window), sceneRoot, window )
    }
    fogPpe.bindShaderMaterialToEntity( entity )
    sender ! ElementInjected( Actor.self, entity )
  }

  private def createSaturationEffect( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList ) {
    val saturation = fresh.firstValueFor( gt.Saturation )
    val saturationPpe = siris.components.renderer.jvr.PostProcessingEffect( "saturation" ).describedByShaders( "pipeline_shader/quad.vs" :: "pipeline_shader/saturation.fs" :: Nil ).usingColorBufferAsName( "jvr_Texture0" ).where( "saturation" ).hasValue( saturation ).isReachableBy( gt.Saturation ).pack

    this.postProcessingEffects = this.postProcessingEffects ::: saturationPpe :: Nil
    this.entityToPostProcessingEffects = this.entityToPostProcessingEffects + (entity -> (saturationPpe::Nil))
    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras(window), sceneRoot, window )
    }
    saturationPpe.bindShaderMaterialToEntity( entity )
    sender ! ElementInjected( Actor.self, entity )
  }

  private def createBloomEffect( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList ) {
    val threshhold = fresh.firstValueFor( gt.Threshold )
    val factor = fresh.firstValueFor( gt.Factor )

    val downSamplingPpe : PostProcessingEffect =
      siris.components.renderer.jvr.PostProcessingEffect( "downsampling" ).
        describedByShaders( "pipeline_shader/quad.vs" :: "pipeline_shader/bright-pass_filter.fs" :: Nil ).
        usingColorBufferAsName( "jvr_Texture0" ).
        where( "threshhold").hasValue( threshhold ).isReachableBy( gt.Threshold ).
        and( "factor" ).hasValue( factor ).isReachableBy( gt.Factor ).pack.
        writesResultIn( "downSampled" ).withRatio( 0.25f )

    val bloomPpe = siris.components.renderer.jvr.PostProcessingEffect( "bloom" ).
      describedByShaders( "pipeline_shader/quad.vs" :: "pipeline_shader/bloom.fs" :: Nil  ).
      usingColorBufferAsName( "jvr_Texture1" ).
      usingFrameBufferObjectWithName( "downSampled" ).as( "jvr_Texture0" )

    this.postProcessingEffects = this.postProcessingEffects ::: downSamplingPpe :: bloomPpe :: Nil
    this.entityToPostProcessingEffects = this.entityToPostProcessingEffects + (entity -> (downSamplingPpe::bloomPpe::Nil))

    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras(window), sceneRoot, window )
    }

    downSamplingPpe.bindShaderMaterialToEntity( entity )

    sender ! ElementInjected( Actor.self, entity )
  }

  private def createDepthOfFieldEffect( sender: Actor, entity : Entity with Removability, aspect : EntityAspect, fresh : SValList ) {
    val intensity = fresh.firstValueFor( gt.Intensity )
    val depthOfFieldPpe = siris.components.renderer.jvr.PostProcessingEffect( "dof" ).
      describedByShaders( "pipeline_shader/quad.vs" :: "pipeline_shader/dof.fs" :: Nil ).
      usingColorBufferAsName( "jvr_Texture1" ).
      usingDepthBufferAsName( "jvr_Texture0" ).
      where( "intensity" ).hasValue( intensity ).isReachableBy( gt.Intensity ).
      pack

    this.postProcessingEffects = this.postProcessingEffects ::: depthOfFieldPpe :: Nil
    this.entityToPostProcessingEffects = this.entityToPostProcessingEffects + (entity -> (depthOfFieldPpe::Nil))

    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras(window), sceneRoot, window )
    }

    depthOfFieldPpe.bindShaderMaterialToEntity( entity )

    sender ! ElementInjected( Actor.self, entity )
  }

  private def createInterface( sender: Actor, entity : Entity with Removability, aspect : EntityAspect ) {
    val interfacePpe = siris.components.renderer.jvr.PostProcessingEffect( "interface" ).
      describedByShaders( "pipeline_shader/quad.vs" :: "shader/interface.fs" :: Nil ).
      isAnOverlay.where( "lives" ).hasValue( 3 ).isReachableBy( gt.Lives ).
      and( "health").hasValue( 1.0f ).isReachableBy( gt.Health ).
      and( "mana" ).hasValue( 1.0f ).isReachableBy( gt.Mana ).
      pack.
      provideImage( "simthief/images/heart.png" ).as( "iface_live" )

    this.postProcessingEffects = this.postProcessingEffects ::: interfacePpe :: Nil
    this.entityToPostProcessingEffects = this.entityToPostProcessingEffects + (entity -> (interfacePpe::Nil))

    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras(window), sceneRoot, window )
    }

    interfacePpe.bindShaderMaterialToEntity( entity )
    sender ! ElementInjected( Actor.self, entity )

  }

  private def createPostProcessingEffect( sender: Actor, entity : Entity with Removability, aspect : EntityAspect ) {
    val ppe = aspect.createParamSet.getFirstValueFor( siris.ontology.types.PostProcessingEffect ).get
    ppe. bindShaderMaterialToEntity( entity )

    this.postProcessingEffects = this.postProcessingEffects ::: ppe :: Nil
    this.entityToPostProcessingEffects = this.entityToPostProcessingEffects + (entity -> (ppe::Nil))

    for( (window,pipeline) <- pipelines ) {
      pipeline.clearPipeline()
      createDefaultPipeline( pipeline, cameras( window), sceneRoot, window )
    }

    sender ! ElementInjected( Actor.self, entity )
  }

  private def charToKey(c: Char): SVarDescription[scala.Boolean, scala.Boolean] = c match {
    case 'a' => siris.ontology.types.Key_a
    case 'b' => siris.ontology.types.Key_b
    case 'c' => siris.ontology.types.Key_c
    case 'd' => siris.ontology.types.Key_d
    case 'e' => siris.ontology.types.Key_e
    case 'f' => siris.ontology.types.Key_f
    case 'g' => siris.ontology.types.Key_g
    case 'h' => siris.ontology.types.Key_h
    case 'i' => siris.ontology.types.Key_i
    case 'j' => siris.ontology.types.Key_j
    case 'k' => siris.ontology.types.Key_k
    case 'l' => siris.ontology.types.Key_l
    case 'm' => siris.ontology.types.Key_m
    case 'n' => siris.ontology.types.Key_n
    case 'o' => siris.ontology.types.Key_o
    case 'p' => siris.ontology.types.Key_p
    case 'q' => siris.ontology.types.Key_q
    case 'r' => siris.ontology.types.Key_r
    case 's' => siris.ontology.types.Key_s
    case 't' => siris.ontology.types.Key_t
    case 'u' => siris.ontology.types.Key_u
    case 'v' => siris.ontology.types.Key_v
    case 'w' => siris.ontology.types.Key_w
    case 'x' => siris.ontology.types.Key_x
    case 'y' => siris.ontology.types.Key_y
    case 'z' => siris.ontology.types.Key_z
    case _ => throw new java.lang.Exception("Could not find a siris.ontology.types.Key for the character '" + c + "'!")
  }
}