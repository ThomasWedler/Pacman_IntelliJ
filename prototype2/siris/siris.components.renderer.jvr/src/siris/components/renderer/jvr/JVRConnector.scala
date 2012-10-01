package siris.components.renderer.jvr

import siris.core.entity.Entity
import siris.components.renderer.setup._
import siris.core.entity.description._
import simplex3d.math.floatm.renamed._
import siris.components.renderer.messages._

import java.lang.Integer
import actors.Actor
import siris.components.renderer.GraphicsComponent
import siris.components.naming.NameIt
import siris.core.component.ComponentConfigured
import siris.components.renderer.createparameter.VRUser
import siris.core.entity.typeconversion.{Converter, ConvertibleTrait}
import simplex3d.math.floatm.ConstMat4f
import de.bht.jvr.math.Matrix4
import siris.core.svaractor.{SVarActorImpl, SVarActorLW}
import siris.core.entity.component._
import de.bht.jvr.core.Transform
import siris.ontology.{EntityDescription, Symbols}
import siris.components.worldinterface.WorldInterface
import java.awt.Color
import de.bht.jvr.util

case class RequestTransformation( sender : Actor, element : String, entity : Entity ) extends JVRMessage
case class TellTransformation( sender : Actor, entity : Entity, transformation : Transform ) extends JVRMessage

case class ElementInjected( sender: Actor, entity : Entity with Removability ) extends JVRMessage
/**
 * This class represents the connector to OntologyJVR.
 *
 * @param componentName The name of the component. Typically 'renderer.
 */
class JVRConnector( val componentName: Symbol ) extends SVarActorLW with GraphicsComponent {
  require( componentName != null, "The parameter 'componentName' must not be 'null'!" )




  private var autoRender: Boolean = false

  /**
   * The list of render actors.
   */
  protected def configure(params: SValList) {}

  private var renderActors = List[JVRRenderActor]()

  /**
   * The list of render actors.
   */
  private var user : List[Entity] = List()

  /**
   * The list of actors that are notified if a render window has been closed.
   */
  private var closeObserver : List[Actor] = List()

  private var configurationOrigin : Option[Actor] = None
  private var renderActorsConfigured = 0


  var openEntityConfigs = Map[Entity with Removability,(Actor,Int)]()
  var openTransformationRequests = Map[Entity,(Actor,SValList)]()

  addHandler[TellTransformation]{ msg : TellTransformation =>
    val (configurationActor, ready) = openTransformationRequests( msg.entity )
    ready.addIfNew( siris.ontology.types.Transformation( JVRConnector.transformConverter.convert( msg.transformation ) ) )
    configurationActor ! GetInitialValuesAns( SVarActorImpl.self, ready )
  }

  addHandler[MeshCreated]{
    msg => msg.configActor ! GetInitialValuesAns( SVarActorImpl.self, msg.initialValues)
  }

  addHandler[GetInitialValuesMsg]{ msg : GetInitialValuesMsg =>
    info( "Intitial value requested for {}", msg.asp.createParamSet.semantics )
    val (ready, remaining) = msg.asp.createParamSet.combineWithValues( msg.p )
    if( !remaining.isEmpty ) {
      if( msg.asp.createParamSet.semantics == Symbols.existingNode ) {
        renderActors.head ! RequestTransformation( Actor.self, msg.asp.createParamSet.firstValueFor( siris.ontology.types.SubElement ), msg.e )
        openTransformationRequests = openTransformationRequests + (msg.e -> (msg.sender,ready) )
      } else if( msg.asp.createParamSet.semantics == Symbols.interface ) {
        ready.addIfNew( siris.ontology.types.Health( 1.0f ) )
        ready.addIfNew( siris.ontology.types.Mana( 1.0f ) )
        ready.addIfNew( siris.ontology.types.Lives( 3 ) )
        msg.sender ! GetInitialValuesAns( SVarActorImpl.self, ready )
      } else if( msg.asp.createParamSet.semantics == Symbols.postProcessingEffect ) {
        val ppe = msg.asp.createParamSet.getFirstCreateParamFor( siris.ontology.types.PostProcessingEffect ).get.value
        for( sVarDescription <- remaining ) ready.addIfNew( h( sVarDescription, ppe ) )
        msg.sender ! GetInitialValuesAns( SVarActorImpl.self, ready )
      }
    } else if (msg.asp.createParamSet.semantics == siris.ontology.Symbols.mesh){
      renderActors.foreach( _ ! CreateMesh(SVarActorImpl.self, msg.e, msg.asp, msg.given, ready, msg.sender))
    } else {
      msg.sender ! GetInitialValuesAns( SVarActorImpl.self, ready )
    }

  }


  // Helper function
  def h[T]( c : ConvertibleTrait[T], ppe : PostProcessingEffect ) : SVal[T] = {
    c(  ppe.getValueForSVarDescription( c ) )
  }

  //add handler react to EntityCompleteMsgs
  addHandler[EntityCompleteMsg]{ msg : EntityCompleteMsg =>
    msg.e.addRemoveObserver(Actor.self)
    if(msg.asp.createParamSet.semantics != Symbols.user) {
      info( "Got new completed entity and distribute it to {} actors", renderActors.size )
      info( "Type {}", msg.asp.createParamSet.semantics )
      if(renderActors.isEmpty){println("WARNING: JVR initialization was not complete before this entity creation request")}
      var outstandingAnswers = 0
      for( r <- renderActors ) {
        r ! PublishSceneElement( Actor.self, msg.e, msg.asp )
        outstandingAnswers = outstandingAnswers + 1
      }
      openEntityConfigs = openEntityConfigs + ( msg.e -> (msg.sender,outstandingAnswers) )
    } else {
      trace( "Adding to list of users " )
      user = user ::: msg.e :: Nil
      msg.sender ! EntityConfigCompleted( Actor.self, msg.e )
    }


  }

  addHandler[ElementInjected]{ msg : ElementInjected =>
    var (configurationActor,outstandingAnswers) = openEntityConfigs( msg.entity )
    outstandingAnswers = outstandingAnswers - 1
    if( outstandingAnswers == 0 ) {
      configurationActor ! EntityConfigCompleted( Actor.self, msg.entity )
      openEntityConfigs = openEntityConfigs - msg.entity
    } else {
      openEntityConfigs = openEntityConfigs + ( msg.entity -> (configurationActor,outstandingAnswers) )
    }
  }


  /*def entityConfigComplete(e: Entity, cParamSet : NamedSValList) = {
    if(cParamSet.semantics != Symbols.user) {
      for( r <- renderActors ) {
        val  syncVar = new SyncVar[Int]
        r ! PublishSceneElement( Actor.self, None, e, cParamSet, syncVar )
        syncVar.take
      }
    } else {
      trace( "Adding to list of users " )
      user = user ::: e :: Nil
    }
  }*/

  addHandler[DetachObject]{
    case msg => for( r <- renderActors ) {
      r ! msg
    }

  }

  addHandler[AttachObject]{
    case msg => for( r <- renderActors ) {
      r ! msg
    }

  }

  addHandler[SetAmbientColor]{
    case msg =>
      for( r <- renderActors ) {
        r ! msg
      }
  }

  addHandler[ConfigureRenderer]{
    case ConfigureRenderer( sender, displaySetup, autoRender, effectsConfiguration ) =>
      configurationOrigin = Some( sender )
      info( "Got configuration" )
      if( JVRConnector.amountOfUser( displaySetup ) == 1 ) {

        //        if(user.isEmpty) {
        info( "1 User, processing" )
        info( "Creating user entity " )
        EntityDescription(
          new VRUser,
          NameIt("User")
        ).realize((e) => {
          for(ra <- renderActors)
            ra ! JVRPublishUserEntity(Actor.self, e)
          WorldInterface.registerEntity('user :: Nil, e )
        })

        for( (id,configs) <- JVRConnector.sortToGroups( JVRConnector.createRenderActorConfigs( displaySetup ) ) ) {
          info( "Creating Render Actor for display group {}", id )
          val renderActor = new JVRRenderActor( id, effectsConfiguration.shadowQuality, effectsConfiguration.mirrorQuality )

          info( "Starting Render Actor" )
          renderActor.start()

          info( "Configuring Render Actor" )
          renderActor ! RenderActorConfigs( configs )

          trace( "Adding render actor to list of render actors" )
          renderActors = renderActors ::: renderActor :: Nil
        }
        //        }
        //        else {
        //          var userEntity = user.reverse.head
        //          trace( "Extracting SVars" )
        //          val viewPlatform = userEntity.get(OntologyJVR.viewplatform).get
        //          val headTransform = userEntity.get(OntologyJVR.headtransform).get
        //
        //
        //        }
      }

      trace( "Setting auto render flag to {}", autoRender )
      this.autoRender = autoRender


  }

  addHandler[JVRRenderActorConfigCompleted] {
    case JVRRenderActorConfigCompleted( sender ) =>
      renderActorsConfigured = renderActorsConfigured + 1
      if( renderActorsConfigured == renderActors.size ) {
        this.configurationOrigin.get ! ComponentConfigured( Actor.self )
      }

  }

  addHandler[RegroupEntity] {
    case m : RegroupEntity =>  for( r <- renderActors ) r ! m

  }


  addHandler[RenderNextFrame]{
    case RenderNextFrame( sender ) =>
      if (renderActors.isEmpty)
        this ! RenderNextFrame(sender)
      else
        for( r <- renderActors ) r ! RenderNextFrame( Actor.self )
  }


  addHandler[PauseRenderer] {
    case PauseRenderer( sender ) =>

  }

  addHandler[ResumeRenderer] {
    case ResumeRenderer( sender ) =>

  }

  addHandler[GetKeyboards] { msg =>
  // Todo: Bad implementation, Fixme
    msg.sender ! Keyboards( Actor.self, for( ra <- renderActors ) yield ra.keyboardEntity )
  }

  addHandler[GetMouses] { msg =>
  // Todo: Bad implementation Fixme
    msg.sender ! Mouses( Actor.self, for( ra <- renderActors ) yield ra.mouseEntity )
  }

  addHandler[GetUser] { msg =>
    msg.sender ! User( Actor.self, user )
  }

  addHandler[JVRRenderWindowClosed] { msg =>
    for( o <- closeObserver ) o ! msg
  }

  addHandler[NotifyOnClose] { case NotifyOnClose( sender ) =>
    closeObserver = closeObserver ::: sender :: Nil
  }

  addHandler[ToggleAutoRender] {
    msg => for( r <- renderActors ) r ! msg
  }

  addHandler[PauseRenderer] {
    msg => for( r <- renderActors ) r ! msg
  }

  addHandler[ResumeRenderer] {
    msg => for( r <- renderActors ) r ! msg
  }

  addHandler[SubscribeForRenderSteps] {
    msg => for( r <- renderActors ) r ! msg
  }

  addHandler[UnsubscribeForRenderSteps] {
    msg => for( r <- renderActors ) r ! msg
  }

  registerConvertibleHint(siris.components.renderer.jvr.types.Transformation)
  registerConvertibleHint(siris.components.renderer.jvr.types.ViewPlatform)
  registerConvertibleHint(siris.components.renderer.jvr.types.HeadTransform)
  registerConvertibleHint(siris.components.renderer.jvr.types.Scale)



  info( "Raised and waiting for configuration" )

  override def removeFromLocalRep( e: Entity ) = {
    require( e != null, "The parameter 'e' must not be 'null'!" )
    for( r <- renderActors ) {
      r ! RemoveSceneElement( Actor.self, e )
    }
  }

  override def shutdown = {
    super.shutdown
    renderActors.foreach( _.shutdown )
  }
}

/**
 * The companion object of the JVRConnector. It contains some functions to interpret the display setup
 * desctiption and transform it into a configuration for a JVRRenderActor.
 */
object JVRConnector {

  /**
   * A main function for some testing.
   *
   * @param arg ignored parameters.
   */
  def main( args: Array[String] ) {
    val desc = laptopDisplayDescription;
    //val desc = laptopDisplayDescription;
    println( "Amount of user: " + amountOfUser( desc ) )
    println( "Amount of windows: " + amountOfRenderWindows( desc ) )
    println( "Amount of anaglyph windows: " + amountOfAnaglyphWindows( desc ) )
    println( "Single window renderer configs: " + createSingleDisplayRenderActorConfigs( desc ) )
    println( "Anaglyph window renderer configs: " + createStereoDisplayRenderActorConfigs( desc ) )
    println( "All window renderer configs: " + createRenderActorConfigs( desc ) )
    val jvrConnector = new JVRConnector( 'renderer )
    jvrConnector.start
    jvrConnector ! ConfigureRenderer( Actor.self, laptopDisplayDescription, true, new EffectsConfiguration( "none", "none" ) )
  }


  /**
   * A laptop display desctiption for testing.
   */
  def laptopDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val displayDesc = new DisplayDesc( Some((1440,900)), (0.331, 0.206), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.5f ) ) ), new CamDesc( 0, Eye.LeftEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  /**
   * A anaglyph laptop display desctiption for testing.
   */
  def laptopAnaglyphDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val leftDisplayDesc = new DisplayDesc( Some((1440,900)), (0.331, 0.206), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.5f ) ) ), new CamDesc( 0, Eye.LeftEye )  )
    val rightDisplayDesc = new DisplayDesc( Some((1440,900)), (0.331, 0.206), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.5f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, leftDisplayDesc :: rightDisplayDesc :: Nil, LinkType.AnaglyphStereo )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  /**
   * A holodeck display description for beuth.
   */
  def holodeckDisplayDescription : DisplaySetupDesc = {
    val displaySetupDesc = new DisplaySetupDesc

    val leftDisplayDesc = new DisplayDesc( Some((1920,1200)), (3.73, 2.33), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 1.165f, -1.17f ) ) ), new CamDesc( 0, Eye.LeftEye )  )
    val rightDisplayDesc = new DisplayDesc( Some((1920,1200)), (3.73, 2.33), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 1.165f, -1.17f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val leftDisplayDevice = new DisplayDevice( None, leftDisplayDesc :: Nil, LinkType.SingleDisplay )
    val rightDisplayDevice = new DisplayDevice( None, rightDisplayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( leftDisplayDevice, 0 )
    displaySetupDesc.addDevice( rightDisplayDevice, 0 )

    displaySetupDesc
  }

  /**
   * This methods interprets a display setup description and returs the amount of users.
   *
   * @param displaySetup The display setup.
   * @return The amount of users in the setup desctiption.
   */
  def amountOfUser( displaySetup: DisplaySetupDesc ) : Integer = {
    val eyeIds = collection.mutable.Set[Integer]()
    for( (id,group) <- displaySetup.deviceGroups ) {
      for( displayDevice <- group.dpys ) {
        for( displayDesc <- displayDevice.displayDescs ) {
          eyeIds += displayDesc.view.camId;
        }
      }
    }
    eyeIds.size
  }

  /**
   * This function returns the amount of render windows in the display setup desctiption.
   *
   * @param displaySetup The display setup desctipion.
   * @return The amount of render windows in the display setup desctiption.
   */
  def amountOfRenderWindows( displaySetup: DisplaySetupDesc ) : Integer = {
    var windows = 0;
    for( (id,group) <- displaySetup.deviceGroups ) {
      for( displayDevice <- group.dpys ) {
        if( displayDevice.linkType == LinkType.AnaglyphStereo ) {
          windows += 1
        } else for( displayDesc <- displayDevice.displayDescs ) {
          windows += 1
        }
      }
    }
    windows;
  }

  /**
   * This function returns the amount of anaglyph render windows in the display setup desctiption.
   *
   * @param displaySetup The display setup desctipion.
   * @return The amount of anaglyph render windows in the display setup desctiption.
   */
  def amountOfAnaglyphWindows( displaySetup: DisplaySetupDesc ) : Integer = {
    var windows = 0;
    for( (id,group) <- displaySetup.deviceGroups ) {
      for( displayDevice <- group.dpys ) {
        if( displayDevice.linkType == LinkType.AnaglyphStereo ) {
          windows += 1
        }
      }
    }
    windows;
  }

  /**
   * This function creates RenderActorConfig messages for all single windows.
   *
   * @param displaySetup The display setup desctiption.
   * @param viewPlatformSVar the SVar of the view platform.
   * @param headTransformSVar The SVar of the head transform.
   * @return A set of all configurations as tupels of display group and config.
   */
  def createSingleDisplayRenderActorConfigs( displaySetup : DisplaySetupDesc) : Set[(Int,RenderActorConfig)] = {
    var configs = Set[(Int,RenderActorConfig)]()
    for( (id,group) <- displaySetup.deviceGroups ) {
      for( displayDevice <- group.dpys ) {
        if( displayDevice.linkType == LinkType.SingleDisplay ) {
          for( displayDesc <- displayDevice.displayDescs ) {
            val v = (id,new RenderActorConfig( Actor.self,
              'xyz,
              displayDevice.hardwareHandle,
              displayDesc.resolution,
              displayDesc.size,
              displayDesc.transformation,
              eyeToEyeToRender( displayDesc.view.eye ),
              true, None ))
            configs += v
          }
        }
      }
    }
    configs
  }

  /**
   * This function creates RenderActorConfig messages for all stereo windows.
   *
   * @param displaySetup The display setup desctiption.
   * @param viewPlatformSVar the SVar of the view platform.
   * @param headTransformSVar The SVar of the head transform.
   * @return A set of all configurations as tupels of display group and config.
   */
  def createStereoDisplayRenderActorConfigs( displaySetup : DisplaySetupDesc) : Set[(Int,RenderActorConfig)] = {
    var configs = Set[(Int,RenderActorConfig)]()
    for( (id,group) <- displaySetup.deviceGroups ) {
      for( displayDevice <- group.dpys ) {
        if( displayDevice.linkType == LinkType.AnaglyphStereo ) {
          val displayDesc = displayDevice.displayDescs( 0 )
          val v = (id, new RenderActorConfig( Actor.self,
            'xyz,
            displayDevice.hardwareHandle,
            displayDesc.resolution,
            displayDesc.size,
            displayDesc.transformation,
            EyeToRender.Both,
            true, Some(StereoType.AnaglyphStereo) ))
          configs += v

        } else if( displayDevice.linkType == LinkType.FrameSequential ) {
          val displayDesc = displayDevice.displayDescs( 0 )
          val v = (id, new RenderActorConfig( Actor.self,
            'xyz,
            displayDevice.hardwareHandle,
            displayDesc.resolution,
            displayDesc.size,
            displayDesc.transformation,
            EyeToRender.Both,
            true, Some(StereoType.FrameSequential) ))
          configs += v

        }
      }
    }
    configs
  }

  /**
   * This function converts from the display setup desription eye to the internal eye enum.
   *
   * @param eye the display setup desctipion eye.
   * @return The value of the internal eye enum.
   */
  def eyeToEyeToRender( eye: Eye.Value ) : EyeToRender.Value = {
    if( eye == Eye.LeftEye )
      return EyeToRender.Left
    EyeToRender.Right
  }

  /**
   * This function converts from the internal eye to the display setup descrition eye enum.
   *
   * @param eyeToRender The internal eye enum value.
   * @return The display setup description eye value.
   */
  def EyeToRenderToEye( eyeToRender: EyeToRender.Value ) : Eye.Value = {
    if( eyeToRender == EyeToRender.Left ) {
      return Eye.LeftEye
    }
    return Eye.RightEye
  }

  /**
   * This function creates RenderActorConfig messages for all windows.
   *
   * @param displaySetup The display setup desctiption.
   * @param viewPlatformSVar the SVar of the view platform.
   * @param headTransformSVar The SVar of the head transform.
   * @return A set of all configurations as tupels of display group and config.
   */
  def createRenderActorConfigs( setupDescription : DisplaySetupDesc) : Set[(Int,RenderActorConfig)] = {
    (createSingleDisplayRenderActorConfigs( setupDescription) union createStereoDisplayRenderActorConfigs( setupDescription))
  }

  def sortToGroups( configs: Set[(Int,RenderActorConfig)] ) : Map[Int,List[RenderActorConfig]] = {
    var transformed : Map[Int,List[RenderActorConfig]] = Map()
    for( (id,config) <- configs ) {
      if( !transformed.contains( id ) ) {
        transformed = transformed + (id -> List[RenderActorConfig]())
      }
      transformed = transformed + (id -> (transformed(id) ::: config :: Nil) )
    }
    transformed
  }

  /**
   * This converters converts between a simplex3d matrix and a transform object of OntologyJVR.
   */
  val transformConverter = new Converter[de.bht.jvr.core.Transform, ConstMat4f] {

    override def canRevert(to: ConvertibleTrait[_], from: ConvertibleTrait[_]) = true

    override def canConvert(from: ConvertibleTrait[_], to: ConvertibleTrait[_]) = true

    override def revert(from: ConstMat4f) : de.bht.jvr.core.Transform =

      new de.bht.jvr.core.Transform(
        new Matrix4(
          from.m00, from.m01, from.m02, from.m03,
          from.m10, from.m11, from.m12, from.m13,
          from.m20, from.m21, from.m22, from.m23,
          from.m30, from.m31, from.m32, from.m33 )
      )

    override def convert(from: de.bht.jvr.core.Transform): ConstMat4f = {
      val matrix = from.getMatrix
      ConstMat4f(
        matrix.get( 0, 0 ), matrix.get( 1, 0 ), matrix.get( 2, 0 ), matrix.get( 3, 0 ),
        matrix.get( 0, 1 ), matrix.get( 1, 1 ), matrix.get( 2, 1 ), matrix.get( 3, 1 ),
        matrix.get( 0, 2 ), matrix.get( 1, 2 ), matrix.get( 2, 2 ), matrix.get( 3, 2 ),
        matrix.get( 0, 3 ), matrix.get( 1, 3 ), matrix.get( 2, 3 ), matrix.get( 3, 3 )
      )
    }
  }

  /**
   * This converters converts between a simplex3d matrix and a transform object of OntologyJVR.
   */
  val transformConverter2 = new Converter[de.bht.jvr.core.Transform, simplex3d.math.floatm.renamed.Mat4x4] {

    override def canRevert(to: ConvertibleTrait[_], from: ConvertibleTrait[_]) = true

    override def canConvert(from: ConvertibleTrait[_], to: ConvertibleTrait[_]) = true

    override def revert(from: simplex3d.math.floatm.renamed.Mat4x4) : de.bht.jvr.core.Transform =

      new de.bht.jvr.core.Transform(
        new Matrix4(
          from.m00, from.m01, from.m02, from.m03,
          from.m10, from.m11, from.m12, from.m13,
          from.m20, from.m21, from.m22, from.m23,
          from.m30, from.m31, from.m32, from.m33 )
      )

    override def convert(from: de.bht.jvr.core.Transform): simplex3d.math.floatm.renamed.Mat4x4 = {
      val matrix = from.getMatrix
      simplex3d.math.floatm.renamed.Mat4x4(
        matrix.get( 0, 0 ), matrix.get( 1, 0 ), matrix.get( 2, 0 ), matrix.get( 3, 0 ),
        matrix.get( 0, 1 ), matrix.get( 1, 1 ), matrix.get( 2, 1 ), matrix.get( 3, 1 ),
        matrix.get( 0, 2 ), matrix.get( 1, 2 ), matrix.get( 2, 2 ), matrix.get( 3, 2 ),
        matrix.get( 0, 3 ), matrix.get( 1, 3 ), matrix.get( 2, 3 ), matrix.get( 3, 3 )
      )
    }
  }

  val colorConverter = new Converter[de.bht.jvr.util.Color, java.awt.Color]{
    /**
     * the actual conversion function
     * @param i the input data to be converted
     * @return the converted data
     */
    def convert(i: util.Color) =
      new Color(i.r*255, i.g*255, i.b*255)

    /**
     * the actual reversion function
     * @param i the input data to be reverted
     * @return the reverted data
     */
    def revert(i: Color) =
      new util.Color(i.getRed/255.0f, i.getGreen/255.0f, i.getBlue/255.0f)
  }
}



/**
 * This enum is used by the internal configuration to describe which eye is rendererd by a window.
 */
object EyeToRender extends Enumeration {

  /**
   * The left eye.
   */
  val Left = Value( "Left" )

  /**
   * The right eye.
   */
  val Right = Value("Right")

  /**
   * Both eyes. Normally this means anaglyph.
   */
  val Both = Value("Both")
}




