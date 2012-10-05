package siris.java

import actors.Actor
import siris.core.component._
import siris.core.svaractor._
import siris.components.renderer.messages._
import siris.components.renderer.createparameter._
import siris.components.renderer.createparameter.convert._
import siris.components.renderer.setup._

import simplex3d.math.floatm.renamed._
import siris.core.entity.Entity
import simplex3d.math.intm.Vec2i
import siris.components.worldinterface.WorldInterface
import java.awt.event._
import simplex3d.math.floatm.FloatMath._
import siris.components.renderer.jvr._
import siris.ontology.{EntityDescription, types, SVarDescription}
import java.util.UUID
//import siris.applications.fishtank.objects.Line
import simplex3d.math.floatm.{Vec3f, Vec2f}
import siris.core.entity.description.SequentialRealize
import siris.core.helper.{SchedulerAdjustment, TimeMeasurement}
import java.awt.Color

/**
 * Created by IntelliJ IDEA.
 * User: dwiebusch
 * Date: 11.01.11
 * Time: 12:02
 * To change this template use File | Settings | File Templates.
 */

//object JavaInterface{
//  def main(args: Array[String]) {}
//}

/**
 * Provides basic graphics- and IO-functionality based on the SimulatorX core.
 * @param ambientColor Tell the renderer to set the ambient color to ambientColor. (This is a temporary hotfix)
 * @param ambientLightingOnly Use ambient lighting only. (This is a hotfix for MacBookAirs)
 */
class JavaInterface (ambientColor: Color = new Color(0.3f, 0.3f, 0.3f), ambientLightingOnly: Boolean = false) {

  /**
   * Provides basic graphics- and IO-functionality based on the SimulatorX core.
   * @param whiteAmbient Tell the renderer to set the ambient color to white. (This is a temporary hotfix)
   * @param ambientLightingOnly Use ambient lighting only. (This is a hotfix for MacBookAirs)
   */
  def this(whiteAmbient: Boolean, ambientLightingOnly: Boolean) = this(if(whiteAmbient) Color.WHITE else new Color(0.3f, 0.3f, 0.3f), ambientLightingOnly)

  private case class StartApp( desc : DisplaySetupDesc )
  private case class LightRequest( )
  private case class CamPosRequest( dist : Float)
  private case class SetCamRequest( aX : Float, aY : Float, aZ : Float, pos : Vec3 )
  private case class PinCamTo( id: UUID, aX : Float, aY : Float, aZ : Float, offset : Vec3 )
  private case class RotateRequest( id : UUID, angle : Float )
  private case class RotateToRequest( id : UUID, angle : Float )
  private case class MoveRequest( id : UUID, x : Float, y : Float, z : Float )
  private case class MoveToRequest( id : UUID, x : Float, y : Float, z : Float )
  private case class LoadRequest(filename : String, initialPos : Vec3, scale : Vec3, id : UUID, alternativeFilename: Option[String])
  private case class LineRequest(fromId: UUID, toId: UUID)
  private case class StaticLineRequest(fromId: UUID, toId: UUID)
  private case class SwapRequest(id: UUID)

  private case class LocalEntityData(
    var entity: Entity,
    var transformation: ConstMat4 = ConstMat4(Mat3x4.Identity),
    var alternative: Option[Entity] = None)

  val lightId = UUID.randomUUID()
  val lightPos = ConstMat4( Mat3x4.translate( ConstVec3(0, 0, 20f ) ) )

  val appActor = new SVarActorHW with TimeMeasurement {
    private val msgQueue = scala.collection.mutable.Map[UUID, List[Any]]()
    private val localData = scala.collection.mutable.HashMap[UUID, LocalEntityData]()

    private val keyListeners = scala.collection.mutable.Set[KeyListener]()
    private val mouseListeners = scala.collection.mutable.Set[MouseListener]()
    private val windowListeners = scala.collection.mutable.Set[WindowListener]()
    private val me = new java.awt.Component{}
    val jvr: JVRConnector = new JVRConnector('renderer)
    private var camera : Option[Entity] = None
    private var lastMousePos = Vec2i(0, 0)
    private var initialDist = 5f

    private val theVoid = ConstMat4(Mat3x4.translate(Vec3f.One) * -1000f)

    private def changeTransform( id : UUID, newValue : ConstMat4 ) = {
      localData.get(id).collect{ case entityData =>
        val oldValue    = entityData.transformation
        val translation = Vec3( oldValue.m03,  oldValue.m13,  oldValue.m23 )
        val postMult    = ConstMat4(Mat3x4.translate(-translation)) * oldValue
        val preMult     = ConstMat4(Mat3x4.translate( translation))
        val newVal      = ConstMat4(preMult * newValue * postMult)

        entityData.transformation = newVal
        entityData.entity.get(types.Transformation).collect{ case t => t.set(newVal) }
      }
    }

    private def setCamDist( dist : Float ) = camera.collect{
      case cam => setCam(45f, 0, 0, Vec3(0, -dist, dist))
    }

    private var camTask : Option[() => Unit] = None

    private def setCam(angleX : Float, angleY : Float, angleZ : Float, position : Vec3) : Unit = camera match {
      case Some(cam) => cam.get(siris.ontology.types.ViewPlatform).get.set( ConstMat4(
        Mat3x4.rotateX(deg2rad(angleX)).rotateY(deg2rad(angleY)).rotateZ(deg2rad(angleZ)).translate( position )
      )  )
      case None => camTask = Some( () => setCam(angleX, angleY, angleZ, position) )
    }

    private def deg2rad( angle : Float ) =
      angle * math.Pi.toFloat / 180f

    private def keyEvent( isDown : Boolean, intValue : Int, charValue : Char) = {
      val event = new KeyEvent(me, -1, System.currentTimeMillis, 0, intValue, charValue)
      if (isDown) for( l <- keyListeners )
        l.keyPressed(event)
      else for( l <- keyListeners ) {
        l.keyReleased(event)
        l.keyTyped(event)
      }
    }

    private def mouseEvent(down : Boolean, buttonInfo : Int, pos : Vec2i) = {
      val event = new MouseEvent(me, -1, System.currentTimeMillis, 0, pos.x, pos.y, pos.x, pos.y, 0, false, buttonInfo)
      lastMousePos = pos
      mouseListeners.foreach{ l =>
        if (down)
          l.mousePressed(event)
        else if (buttonInfo == MouseEvent.NOBUTTON)
          l.mouseEntered(event)
        else {
          l.mouseReleased(event)
          l.mouseClicked(event)
        }
      }
    }

    addHandler[PinCamTo]{ msg => {
      if (localData.contains(msg.id))
        localData.get(msg.id).collect{ case eData =>
          eData.entity.get(siris.ontology.types.Transformation).get.observe( entityTrans => {
            val entityPos = entityTrans(3).xyz
            setCam(msg.aX, msg.aY, msg.aZ, entityPos + msg.offset)
          })
          eData.entity.get(siris.ontology.types.Transformation).get.get( entityTrans => {
            val entityPos = entityTrans(3).xyz
            setCam(msg.aX, msg.aY, msg.aZ, entityPos + msg.offset)
          })
        }
      else
        msgQueue.update(msg.id, msg :: msgQueue.getOrElseUpdate(msg.id, Nil))
    }}

    addHandler[User] {
      case User( sender, cam :: tail ) =>
        camera = Some(cam)
        if (camTask.isEmpty) setCamDist( initialDist ) else camTask.get.apply
      case User( sender, Nil ) => jvr ! GetUser(Actor.self)
    }

    addHandler[CamPosRequest]{
      msg => if (camera.isDefined) setCamDist(msg.dist) else initialDist = msg.dist
    }

    addHandler[SetCamRequest]{ msg =>
      setCam( msg.aX, msg.aY, msg.aZ, msg.pos )
    }

    addHandler[StartApp]{ msg =>
      jvr ! ConfigureRenderer(Actor.self, msg.desc, true, new EffectsConfiguration( "none", "none" ) )
      jvr.start
      jvr ! NotifyOnClose( Actor.self )
      jvr ! GetUser( Actor.self )
      jvr ! GetKeyboards( Actor.self )
      jvr ! GetMouses( Actor.self )
      jvr ! RenderNextFrame(Actor.self)

      scheduleExecution(1000L) {
        if(!ambientLightingOnly) Actor.self ! LightRequest()
       // println(ambientColor.getRed)
        val color =
          new de.bht.jvr.util.Color(
            ambientColor.getRed.toFloat / 255f,
            ambientColor.getGreen.toFloat / 255f,
            ambientColor.getBlue.toFloat / 255f)
        jvr ! SetAmbientColor(Actor.self, color)
      }
    }

    addHandler[KeyListener] {
      listener => keyListeners += listener
    }

    addHandler[MouseListener]{
      listener => mouseListeners += listener
    }

    addHandler[WindowListener]{
      listener => windowListeners += listener
    }

    addHandler[Keyboards] { msg =>
      if (msg.keyboards.isEmpty)
        jvr ! GetKeyboards(Actor.self)
      else
        msg.keyboards.headOption collect {
          case kb => if (kb == null) jvr ! GetKeyboards( Actor.self ) else {
            if (kb.get(types.Key_Down).isDefined){
              kb.get(types.Key_Up).get.observe( keyEvent(_, KeyEvent.VK_UP, 0) )
              kb.get(types.Key_Left).get.observe( keyEvent(_, KeyEvent.VK_LEFT, 0) )
              kb.get(types.Key_Down).get.observe( keyEvent(_, KeyEvent.VK_DOWN, 0) )
              kb.get(types.Key_Right).get.observe( keyEvent(_, KeyEvent.VK_RIGHT, 0) )
              kb.get(types.Key_Space).get.observe( keyEvent(_, KeyEvent.VK_SPACE, 0) )
              for (char <- 'a' to 'z')
                kb.get(charToKey(char)).get.observe{
                  (pressed: Boolean) => keyEvent(pressed, char.toInt, char)
                }
            }
            else jvr ! GetKeyboards( Actor.self )
          }
        }
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

    addHandler[Mouses]{ msg =>
      if (msg.mouses.isEmpty)
        jvr ! GetMouses(Actor.self)
      else
        msg.mouses.headOption collect {
          case mouse => if (mouse == null) jvr ! GetMouses( Actor.self ) else {
            if (mouse.get(types.Button_Center).isDefined){
              mouse.get(types.Position2D).get.observe{ (pos) => mouseEvent( false, MouseEvent.NOBUTTON, Vec2i(pos.x.toInt, pos.y.toInt)) }
              mouse.get(types.Button_Left).get.observe{   mouseEvent( _, MouseEvent.BUTTON1, lastMousePos) }
              mouse.get(types.Button_Center).get.observe{ mouseEvent( _, MouseEvent.BUTTON2, lastMousePos) }
              mouse.get(types.Button_Right).get.observe{  mouseEvent( _, MouseEvent.BUTTON3, lastMousePos) }
            }
            else
              jvr ! GetMouses( Actor.self )
          }
        }
    }

    addHandler[JVRRenderWindowClosed]{ case JVRRenderWindowClosed( sender ) =>
      WorldInterface.shutdown
      Component.shutdownAll
      SVarActorImpl.self.shutdown
      windowListeners.foreach( _.windowClosed(null) )
      Thread.sleep(500)
      sys.exit(0)
    }

    addHandler[LightRequest] { msg =>
      localData += lightId -> LocalEntityData(
          EntityDescription(
            new SpotLight(
              name = "light1",
              transformation = lightPos,
              diffuseColor   = new Color(0.5f, 0.6f, 0.5f),
              specularColor  = new Color(0.5f, 0.6f, 0.5f)
            ) where (types.Transformation isProvided)
          ).realize,
          lightPos
        )
    }

    addHandler[LoadRequest]{ msg =>
      (SequentialRealize(
        EntityDescription(
          new ShapeFromFile(
            file = msg.filename,
            transformation = ConstMat4(Mat3x4.translate(msg.initialPos)),
            scale = ConstMat4( Mat3x4.scale(msg.scale) )
          ).where(types.Transformation isProvided)
        )
      ).whereResultIsProcessedBy(registerNewEntity(msg, _)) :::
      msg.alternativeFilename.map(altFilename =>
        SequentialRealize(
          EntityDescription(
            new ShapeFromFile(
              file = altFilename,
              transformation = theVoid,
              scale = ConstMat4( Mat3x4.scale(msg.scale) )
            ).where(types.Transformation isProvided)
          )
        ).whereResultIsProcessedBy(registerAlternative(msg, _))
      ).getOrElse(SequentialRealize())).execute
    }

    private def registerNewEntity( msg : LoadRequest,  e : Entity ) = {
      localData += msg.id -> LocalEntityData(e, ConstMat4(Mat3x4.translate(msg.initialPos)))
      msgQueue.get(msg.id).collect{ case queue =>
        queue.filter(!_.isInstanceOf[SwapRequest]).foreach(applyHandlers(_))
        val remaining = queue.filter(_.isInstanceOf[SwapRequest])
        if(remaining.isEmpty) msgQueue.remove(msg.id)
        else msgQueue.update(msg.id, remaining)
      }
    }

    private def registerAlternative( msg : LoadRequest,  e : Entity ) = {
      localData.get(msg.id).get.alternative = Some(e)
      msgQueue.remove(msg.id).collect{ case queue => queue.foreach(applyHandlers(_)) }
    }

    addHandler[MoveRequest]{msg =>
      if (localData.contains(msg.id))
        changeTransform(msg.id, ConstMat4(Mat3x4.translate( Vec3(msg.x, msg.y, msg.z) ) ) )
      else
        msgQueue.update(msg.id, msg :: msgQueue.getOrElseUpdate(msg.id, Nil))
    }

    addHandler[MoveToRequest]{ msg =>
      if (localData.contains(msg.id)){
        val entityData  = localData.get(msg.id).get
        val oldValue    = entityData.transformation
        val postMult    = ConstMat4( Mat3x4.translate(-Vec3( oldValue.m03,  oldValue.m13,  oldValue.m23 )) ) * oldValue
        val newVal      = ConstMat4( Mat3x4.translate( Vec3( msg.x, msg.y, msg.z ) ) * postMult )

        entityData.entity.get(types.Transformation).collect{ case t=>t.set(newVal) }
        entityData.transformation = newVal
      }
      else
        msgQueue.update(msg.id, msg :: msgQueue.getOrElseUpdate(msg.id, Nil))
    }

    addHandler[LineRequest]{ msg =>
      if (localData.contains(msg.fromId) && localData.contains(msg.toId)){
        new Line(
          localData.get(msg.fromId).get.entity.get(types.Transformation).get,
          localData.get(msg.toId).get.entity.get(types.Transformation).get)
        msgQueue.update(msg.fromId, msgQueue.getOrElseUpdate(msg.fromId, Nil).filter(_ != msg))
        msgQueue.update(msg.toId, msgQueue.getOrElseUpdate(msg.toId, Nil).filter(_ != msg))
      }
      else {
        msgQueue.update(msg.fromId, msg :: msgQueue.getOrElseUpdate(msg.fromId, Nil))
        msgQueue.update(msg.toId, msg :: msgQueue.getOrElseUpdate(msg.toId, Nil))
      }
    }

    addHandler[StaticLineRequest]{ msg =>
      if (localData.contains(msg.fromId) && localData.get(msg.toId).isDefined){
        val fromData = localData.get(msg.fromId).get
        val toData = localData.get(msg.toId).get
        fromData.entity.get(types.Transformation).get.get(fromTrans => {
          toData.entity.get(types.Transformation).get.get(toTrans => {
            new Line(SVarImpl(fromTrans), SVarImpl(toTrans))
          })
        })
        msgQueue.update(msg.fromId, msgQueue.getOrElseUpdate(msg.fromId, Nil).filter(_ != msg))
        msgQueue.update(msg.toId, msgQueue.getOrElseUpdate(msg.toId, Nil).filter(_ != msg))
      }
      else {
        msgQueue.update(msg.fromId, msg :: msgQueue.getOrElseUpdate(msg.fromId, Nil))
        msgQueue.update(msg.toId, msg :: msgQueue.getOrElseUpdate(msg.toId, Nil))
      }
    }

    addHandler[RotateToRequest]{ msg =>
      if (localData.contains(msg.id)){
        val entityData  = localData.get(msg.id).get
        val oldValue    = entityData.transformation
        val translation = Vec3( oldValue.m03,  oldValue.m13,  oldValue.m23 )
        val newVal      = ConstMat4( Mat3x4.rotateZ( msg.angle ).translate( translation ) )

        entityData.entity.get(types.Transformation).collect{ case t=>t.set(newVal) }
        entityData.transformation = newVal
      }
      else
        msgQueue.update(msg.id, msg :: msgQueue.getOrElseUpdate(msg.id, Nil))
    }

    addHandler[RotateRequest]{ msg =>
      if (localData.contains(msg.id))
        changeTransform(msg.id, ConstMat4( Mat3x4.rotateZ( msg.angle*math.Pi.toFloat/180f )) )
      else
        msgQueue.update(msg.id, msg :: msgQueue.getOrElseUpdate(msg.id, Nil))
    }

    private def swap(id: UUID) {
      localData.get(id).collect{case entityData =>
        if(entityData.alternative.isDefined) {
          val tmp = entityData.alternative.get
          entityData.alternative = Some(entityData.entity)
          entityData.entity = tmp
          entityData.entity.get(types.Transformation).collect{case t=>t.set(entityData.transformation)}
          entityData.alternative.get.get(types.Transformation).collect{case t=>t.set(theVoid)}
        }
      }
    }

    addHandler[SwapRequest]{ msg =>
      if (localData.contains(msg.id) && localData.get(msg.id).map(_.alternative.isDefined).getOrElse(false))
        swap(msg.id)
      else
        msgQueue.update(msg.id, msg :: msgQueue.getOrElseUpdate(msg.id, Nil))
    }
  }

  /**
   * Loads a graphical object from file.
   * @param filename A path to a collada file.
   * @param x The initial x position of the object.
   * @param y The initial y position of the object.
   * @param z The initial z position of the object.
   * @param scaleX The x component of a additional scale, that is applied to the loaded object.
   * @param scaleY The y component of a additional scale, that is applied to the loaded object.
   * @param scaleZ The z component of a additional scale, that is applied to the loaded object.
   * @param id The id that shall be used to identify the object later on. The id is generated randomly if it is omitted.
   * @param alternativeFilename An alternative collada file, used by the swap operation.
   * @return The id that identifies the loaded object for later operations. (If an id was passed the return value is equal to the passed id.)
   * @see [[siris.java.JavaInterface.swap()]]
   */
  def loadObject(
    filename : String,
    x : Float,
    y : Float,
    z : Float ,
    scaleX : Float = 1f,
    scaleY : Float = 1f,
    scaleZ : Float = 1f,
    id: UUID = UUID.randomUUID(),
    alternativeFilename: Option[String] = None) : UUID  =
  {
    appActor ! LoadRequest(filename, Vec3(x, y, z), Vec3f(scaleX, scaleY, scaleZ), id, alternativeFilename)
    return id
  }

  /**
   * Starts the graphical output into a window of size (width, height).
   * @param width The width of the window.
   * @param height The height of the window.
   */
  def startRenderer(width : Int, height : Int) =
    appActor ! StartApp( customSingleWindowFixedBigViewPort(width, height) )

  /**
   * Adds a KeyListener to handel keyboard events on the applications window.
   * @param l The keylistener to add.
   */
  def addKeyListener(l : KeyListener) =
    appActor ! l

  /**
   * Adds a WindowListener to handel events on the applications window.
   * @param l The WindowListener to add.
   */
  def addWindowListener( l : WindowListener ) =
    appActor ! l

  /**
   * Sets the virtual camera.
   * @param ax Rotation around x in degrees.
   * @param ay Rotation around y in degrees.
   * @param az Rotation around z in degrees.
   * @param posX The x component of the position.
   * @param posY The y component of the position.
   * @param posZ The z component of the position.
   */
  def setCamTo( ax : Float, ay : Float, az : Float, posX : Float, posY : Float, posZ : Float) =
    appActor ! SetCamRequest(ax, ay, az, Vec3(posX, posY, posZ) )

  /**
   * Pins the virtual camera to another entity.
   * @param id The id of the entity to which the virtual camera is pinned to.
   * @param aX Rotation (of the virtual camera) around x in degrees.
   * @param aY Rotation (of the virtual camera) around y in degrees.
   * @param aZ Rotation (of the virtual camera) around z in degrees.
   * @param offsetX The x component of the position offset (of the virtual camera).
   * @param offsetY The y component of the position offset (of the virtual camera).
   * @param offsetZ The z component of the position offset (of the virtual camera).
   */
  def pinCamTo( id: UUID, aX : Float, aY : Float, aZ : Float, offsetX: Float,  offsetY: Float, offsetZ: Float ) {
    appActor ! PinCamTo(id,aX,aY,aZ,Vec3(offsetX,offsetY,offsetZ))
  }

  /**
   * Moves an object to a new position.
   * @param id The id of the object to move.
   * @param x The x component of the new position.
   * @param y The y component of the new position.
   * @param z The z component of the new position.
   */
  def moveObjectTo( id : UUID, x : Float, y : Float, z : Float) =
    appActor ! MoveToRequest(id, x, y, z)

  /**
   * Moves an object by a given vector.
   * @param id The id of the object to move.
   * @param x The x component of the vector.
   * @param y The y component of the vector.
   * @param z The z component of the vector.
   */
  def moveObject( id : UUID, x : Float, y : Float, z : Float ) : Unit =
    appActor ! MoveRequest(id, x, y, z)

  /**
   * Rotates an object to a new angle (around the z-axis).
   * @param id The id of the object to rotate.
   * @param angle The angle to rotate the object to.
   */
  def rotateObjectTo( id : UUID, angle : Float ) =
    appActor ! RotateToRequest(id, angle)

  /**
   * Rotates an object by a given angle (around the z-axis).
   * @param id The id of the object to rotate.
   * @param angle The angle to rotate the object by.
   */
  def rotateObject( id : UUID, angle : Float ) : Unit =
    appActor ! RotateRequest(id, angle)

  /**
   * Sets the camera above the x-y-plane, using a view direction that points to (0,0,0) at a 45 deg angle.
   * @param dist The distance of the camera to the x-y-plane.
   */
  def setCamDist( dist : Float ) =
    appActor ! CamPosRequest(dist)

  /**
   * Adds a MouseListener to handel mouse events on the applications window.
   * @param l The MouseListener to add.
   */
  def addMouseListener( l : MouseListener ) =
    appActor ! l

  /**
   * Adds a line between two objects.
   * @param fromId The id of the first object.
   * @param toId The id of the second object.
   */
  def addLine(fromId: UUID, toId: UUID) =
    appActor ! LineRequest(fromId, toId)

  /**
   * Adds a line between the positions of two objects.
   * @param fromId The id of the first object.
   * @param toId The id of the second object.
   */
  def addStaticLine(fromId: UUID, toId: UUID) =
    appActor ! StaticLineRequest(fromId, toId)

  /**
   * Swaps the graphical representation of an object.
   * @param id The id of the object to swap.
   * @see [[siris.java.JavaInterface.loadObject()]]
   */
  def swap(id: UUID) =
    appActor ! SwapRequest(id)

  def customSingleWindowFixedBigViewPort(widthInPx: Int, heightinPx: Int) :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val oneMeterInInches = 39.3700787
    val dpi: Double = 96.0 * 0.5 // the factor 0.5 lets the user see "more scene" in the window
    val widthOnScreenInMeter = 1920.0 / (dpi * oneMeterInInches)


    val displayDesc = new DisplayDesc( Some((widthInPx,heightinPx)), (widthOnScreenInMeter, widthOnScreenInMeter * ( 1080.0 / 1920.0)), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )
    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  appActor.start

}