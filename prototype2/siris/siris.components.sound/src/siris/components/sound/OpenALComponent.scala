package siris.components.sound

import siris.core.entity.Entity
import siris.core.svaractor.SVarActorHW
import siris.components.eventhandling._
import siris.ontology.{types, Symbols}
import scala.collection._
import scala.xml._
import java.io.{FilenameFilter, File}
import java.lang.String
import siris.components.worldinterface.{CreationMessage, WorldInterface}
import siris.core.component.ConfigureComponentMessage
import simplex3d.math.floatm.{Mat4f, ConstVec3f, Vec3f, ConstMat4f}
import siris.components.physics.PhysicsEvents
import siris.core.entity.description.{EntityAspect, SValList, NamedSValList}
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.entity.component.{EntityConfigLayer, Removability}

/**
 * User: anke
 * Date: 04.02.11
 * Time: 13:14
 */

class OpenALComponent(confParam: SValList, override val componentName : Symbol)
  extends SVarActorHW with SoundComponent with EventHandler with EntityConfigLayer{

  // initialize OpenAL
  info("Init OpenAL")
  OpenALUtils.alInit();

  protected def configure(params: SValList) {}

  var fancypants: OpenALWave = null
  var source: OpenALSource = new OpenALSource
  var soundConfigFileName: Option[String] = None
  var soundEvents: Option[List[EventDescription]] = None
  var camTransform : ConstMat4f = ConstMat4f(Mat4f.Identity)

  object Command extends Enumeration {
    val Play, Stop, Pause = Value
  }

  private val entityMap  = mutable.Map[Entity, SoundEntity]()
  private val materialMap  = mutable.Map[(Symbol,Symbol), SoundObject]()
  private val eventHandlingFunctions = mutable.Set[Function1[Event,Unit]]()
  private val requestedEvents = mutable.Set[EventDescription]()

  // println(confParam.getFirstValueFor(OpenALComponent.soundEvents))

  /*constructor*/    //TODO: move to addHandler[ConfigureComponentMessage]

  soundConfigFileName = confParam.getFirstValueFor(OpenALComponent.soundConfigFile)
  soundEvents = Some(confParam.getAllValuesFor(OpenALComponent.soundEvent))


  override def startUp() {



    //
    //materialMap += ('ghost, 'ice) -> new SoundObject("sounds/ghost.wav")
    //materialMap += ('ice, 'ghost) -> new SoundObject("sounds/ghost.wav")
    // load the sounds
    //    try {
    //      fancypants = OpenALWave.create("sounds/FancyPants.wav")
    //    }
    //    catch{
    //      case e: Exception =>
    //        println("Failed starting OpenAL due to ")
    //        e.printStackTrace();
    //        System.exit(-1);
    //    }

    if(soundConfigFileName.isDefined) loadSoundConfig(soundConfigFileName.get)

    //    generate all OpenAL sound sources
    // OpenALWave.alGenWaves();

    //  set OpenAL listener position
    WorldInterface.registerForCreationOf(List('camera))
    OpenALListener.setPos(0, 0, 0);


  }



  addHandler[CreationMessage]{
    msg => msg.path match {
      case 'camera :: _ =>  msg.e.get(siris.ontology.types.ViewPlatform).collect {
        case svar =>
          svar.observe {
            t => {
              camTransform = t
            }
          }
      }
      case _ => {}
    }
  }

  addHandler[ConfigureComponentMessage]{
    msg: ConfigureComponentMessage =>  {}
    /*  msg.createParams.getFirstValueFor(Editor.name).collect{case name => conf.name = name}
    msg.createParams.getFirstValueFor(Editor.appName).collect{
      case name =>
        conf.appName = name
        publish(AppNameChanged(name))
    }*/
  }

  /* addHandler[RegisterCam]{ msg =>
    WorldInterface.handleRegisteredEntities('camera :: Nil)( _ match{
      case Some(set) => set.headOption match {
        case Some(cam) => cam.get(OntologyJVR.viewplatform).collect{ case t => t.observe{
          transform => camPosition = OntologyJVR.transformConverter.convert(transform)
        }}
        case None => this ! RegisterCam()
      }
      case None => this ! RegisterCam()
    })
  }*/

  registerConvertibleHint(types.Transformation)


  /**
   * Implemented method
   * @see siris.components.eventhandling.EventHandler
   */
  def handleEvent(e: Event) {

    /*handle every event e*/
    eventHandlingFunctions.foreach(_ (e))
  }

  /**
   * Implemented method
   * @see siris.core.entity.component.EntityConfigLayer
   */
  def removeFromLocalRep(e:Entity) {
    //Todo : delete Entities in maps
    entityMap.remove(e)
    //println("delete entity: " + e.toString)

  }

  protected def requestInitialValues(toProvide: immutable.Set[ConvertibleTrait[_]], aspect: EntityAspect,
                                     e: Entity, given: SValList){
    provideInitialValues( e, SValList() )
  }

  override protected def entityConfigComplete(e: Entity with Removability, aspect: EntityAspect) {
    aspect.createParamSet.semantics match {
      case SoundSymbols.soundObjects => {
        val soundEntity = entityMap.getOrElseUpdate(e, createNewSoundEntity(e, aspect.createParamSet))
        if(soundEntity == null)
          println("soundentity not defined")
        soundEntity.svar = e.get(types.Transformation)
      }
      case SoundSymbols.soundListener => {
        //Todo: if Player/Cam/User is realized as an Entity with a CreateParamterSet implement this
      }
    }
  }

  /**
   * Parse the sound config file. Preload sound files and save material settings.
   * @param file path of sound config file (.xml)
   */
  def loadSoundConfig(fileName : String) {
    val file = new File(fileName)
    if (!file.exists) {
      error("Can't load SoundCofig file \"" + file.getAbsolutePath + "\". Check the file path.", this) }
    else {
      val root = scala.xml.XML.loadFile(fileName)

      /*preload all soundfiles of soundFolder*/
      val soundFolder = root \ "soundFolder"
      if (soundFolder.isDefinedAt(0)) preloadSoundfilesFromFolder(soundFolder.head.text)

      /*register new MaterialSoundObjects*/
      val materialPair = root \\ "materialPair"
      materialPair.foreach((n: Node) =>
        materialMap += ((Pair(Symbol((n \ "@material1").head.text), Symbol((n \ "@material2").head.text)) -> new SoundObject((n \ "@soundFile").head.text)))
      )

      /*register new EventHandling Sound Object*/
      if (soundEvents.isDefined){
        val soundEvent = root \\ "soundEvent"
        soundEvent.foreach((n: Node) => {
          var fileName = ""
          var looping = false
          var commands = List[(Command.Value, String)]()

          n.attributes.foreach((attr : MetaData) => {
            //println(attr.key + "; " + attr.toString + "; " + attr.value.head.text)

            attr.key match {
              case "play" => commands = (Command.Play, attr.value.head.text) :: commands
              case "pause" => commands = (Command.Pause, attr.value.head.text) :: commands
              case "stop" => commands = (Command.Stop, attr.value.head.text) :: commands
              case "soundFile" => fileName = attr.value.head.text
              case "looping" => looping = attr.value.head.text.toBoolean
              case _ => {}
            }
          })

          val so = new SoundObject(fileName, looping)

          commands.foreach( (pair) => {
            soundEvents.collect {
              case list => list.find((item) => {
                item.name.value.toSymbol.name == pair._2
              }).collect {
                case eventDescr => registerNewEventHandler(eventDescr, so, pair._1)//; println("register " + pair._1.toString)
              }
            }})

          /*println( (n \ "@play").head.text + " " + (n \ "@soundFile").head.text )
          soundEvents.get.find( (item) => {ConvertibleTrait.getSemantics(item.name).name  == (((n \ "@play").head.text.trim)) }
          ).collect {
            case eventDescr => registerNewEventHandler(eventDescr, new SoundObject((n \ "@soundFile").head.text), Command.Play)
          }*/
        })
      }
    }
  }

  /**
   * Preload all soundFiles (.wav) of this folder in OpenAL
   * @param name of folder where soundfiles (.wav) are stored
   */
  private def preloadSoundfilesFromFolder(folderName : String) {
    val folder = new File(folderName)
    if (!folder.exists) {
      error("Can't open folder \"" + folder.getAbsolutePath + "\". Check the path.", this)
      return
    } else {
      val wavFiles = folder.listFiles(new FilenameFilter() {
        def accept(directory: File, filename: String) = {
          filename.endsWith('.' + "wav");
        }
      })
      wavFiles.foreach((f: File) =>
        try {
          OpenALWave.create(f.getPath)
        }
        catch {
          case _ => error("Error while loading .wav file: " + f.getPath )
        })
      OpenALWave.alGenWaves()
    }

  }

  /**
   * Handle the create parameters c.
   * According to these create a new SoundEntity and add new EventHandling functions.
   * @param entity: the entity which refers to the new SoundEntity
   * @param c: the constructor parameters for the new SoundEntity
   * @return the new SoundEntity, null when creation fails
   */
  private def createNewSoundEntity(entity: Entity, c : NamedSValList) : SoundEntity = {

    try {
      val se = new SoundEntity

      /*handle ON_COLLISION_PARAMS*/
      c.getFirstValueFor(OpenALComponent.onCollisionParam).collect {
        case onCollisionParams =>
          se.material = onCollisionParams.firstValueFor(OpenALComponent.onCollision.soundMaterial)

          /*register for playing on collision event*/
          if (onCollisionParams.getFirstValueForOrElse(OpenALComponent.onCollision.play)(false)) {
            /*find only these collisions which are affected by this entity*/
            val eventDescr = PhysicsEvents.collision.restrictedBy {
              case ev => ev.affectedEntities.contains(entity)
            }
            requestEvent(eventDescr)
            eventHandlingFunctions.add(
              (event: Event) => {
                if (eventDescr.name == event.name){
                  entityMap.get(event.affectedEntities.toList(0)).collect {
                    case sEntity1 =>
                      entityMap.get(event.affectedEntities.toList(1)).collect {
                        case sEntity2 =>
                          materialMap.find((item) => {
                            ((item._1._1 == sEntity1.material) && (item._1._2 == sEntity2.material)) ||
                              ((item._1._2 == sEntity1.material) && (item._1._1 == sEntity2.material))
                          }).collect {
                            case materialSoundOpjPair =>
                              if(event.affectedEntities.toList(0) == entity)
                              {playCollisionSound(materialSoundOpjPair._2, sEntity1, sEntity2); println("collision: " + sEntity1.material + " - " + sEntity2.material)}
                          }
                      }
                  }
                }
              })
          }
      }

      /*handle ON_CREATION_PARAMS*/
      c.getFirstValueFor(OpenALComponent.onCreationParam).collect {
        case onCreationParams =>
          se.onCreation = Option(new SoundObject(onCreationParams))
          playSound(se.onCreation.get, se)
      }

      /*handle ON_EVENT_PARAMS*/
      c.getFirstValueFor(OpenALComponent.onEventParam).collect {
        case s : SValList =>
          val so = new SoundObject(s)
          s.getFirstValueFor(types.Container).collect{
            case list => list.getAllValuesFor(OpenALComponent.onEvent.events).foreach {
              ( eventDescr : EventDescription) => {
                registerNewEventHandler(eventDescr, so, Command.Play, Option(se))
              }
            }
          }
      }

      //se +=
      //TODO: implement event handling method


      entityMap += entity -> se
      se
    }
    catch {
      case _ =>
        println("Error while creating a new SoundEntity")
        null
    }

  }

  private def playSound(so : SoundObject, e1 : SoundEntity) {
    /*setListenerPosition*/
    // OpenALListener.setPos(camPosition.x, camPosition.y, camPosition.z);
    //println("camPos: " + camPosition)

    /*setSoundPosition*/
    e1.svar match {
      case Some(sVar) => {

        sVar.get(transform => {
          //println("change entuty pos " + transform.apply(3).xyz)
          //so.setPosition(transform.apply(3).xyz)
          /*playSound*/
          println("play sound of " + e1.material)
          so.play()
        })}
      case _ => {so.play()}
    }
  }

  private def playCollisionSound(so : SoundObject, e1 : SoundEntity, e2 : SoundEntity) {
    /*setListenerPosition*/
    // OpenALListener.setPos(camPosition.x, camPosition.y, camPosition.z);
    //println("camPos: " + camPosition)

    /*setSoundPosition*/
    e1.svar match {
      case Some(sVar) => {

        sVar.get(transform => {
          //println("change entuty pos " + transform.apply(3).xyz)
          //so.setPosition(transform.apply(3).xyz)
          /*playSound*/
          //    println("play sound of " + e1.material)
          so.play()
        })}
      case _ => {so.play()}
    }
  }

  private def playSoundAt(so : SoundObject, pos : Option[ConstVec3f] ) {
    /*setListenerPosition*/
    //    val camPosition = camTransform.apply(3).xyz
    //    val upVector = ConstVec3f(camTransform.apply(0,1), camTransform.apply(1,1), camTransform.apply(2,1))
    //    val atPos = camPosition + normalize(ConstVec3f(-camTransform.apply(0,2), -camTransform.apply(1,2), -camTransform.apply(2,2)))

    //    OpenALListener.setPos(camPosition.x, camPosition.y, camPosition.z);println("camPos " + camPosition )
    //    println("up " + upVector )
    //    println("at " + atPos )
    //
    //    OpenALListener.setOrientation(upVector.x, upVector.y, upVector.z, atPos.x, atPos.y, atPos.z)

    /*setSoundPosition*/
    //    if(pos.isDefined) pos.collect {
    //      case p => so.setPosition(p); println("set Pos: " + p)
    //    }

    //so.setPosition(pos)
    // println("play simple sound")
    so.play()
  }

  /*  eventHandlingFunctions.add((event: Event) => {
    println("SoundComponent is handling event " + event.name)
  })*/

  private def registerNewEventHandler(eventDescr : EventDescription, so : SoundObject, com : Command.Value, se : Option[SoundEntity] = None) {

    /*requestEvent only once*/
    //println("request event: " + eventDescr.name)
    requestEvent(eventDescr)
    requestedEvents += eventDescr


    /*register event with no entity binding*/
    if(!se.isDefined) {
      /*check if position paramter are given*/
      /* if (ConvertibleTrait.getTypeInfo(eventDescr.name) == Ontology.vector3.typeinfo) {*/ //FIXME
      //  println("simple event handling for: " + eventDescr.name)
      /*define HandelEvent function*/
      eventHandlingFunctions.add(
        (event: Event) => {
          if (event.name == eventDescr.name) {
            //case thisEvent => {
            //      println("manage event " + eventDescr.name + " " + com.toString)
            com match {
              case (Command.Play) => {
                val position = event.get(types.Vector3)
                if (position.isDefined) position.collect{
                  case pos =>
                    if(pos == Vec3f.Zero) playSoundAt(so, None)
                    else playSoundAt(so, Option(pos))
                }
                else playSoundAt(so, None/*thisEvent.value.asInstanceOf[Ontology.vector3.dataType]*/)
              } //FIXME
              case (Command.Stop) => so.stop()
              case (Command.Pause) => so.pause()
            }
          }

        })
      /* }*/


      /*event has an entity binding - register it*/
    } else {

      se.get.onEvent += eventDescr -> so // register event in its Soundentity
      //println("entity bound event handling for: " + eventDescr.name)
      /*define HandelEvent function*/
      eventHandlingFunctions.add(
        (event: Event) => {
          if (event.name == eventDescr.name){
            //eventDescr.upcast(event).collect {
            //case thisEvent: Event =>
            event.affectedEntities.foreach((entity) => entityMap.find((item) => item._1 == entity).collect {
              case entityToSoundEntity => entityToSoundEntity._2.onEvent.filter((item) => item._1 == eventDescr).foreach(
                (eventDToSObject) => {
                  com match{
                    case (Command.Play) => playSound(eventDToSObject._2, entityToSoundEntity._2)
                    case (Command.Stop) => eventDToSObject._2.stop()
                    case (Command.Pause) => eventDToSObject._2.pause()
                  }
                }
              )
            })
          }
        })
    }
    /*
    println("register event")
    requestEvent(eventDescr)
    eventHandlingFunctions.add(
      (event: Event) => {
        println("expl event")
        eventDescr.upcast(event).collect {
          case thisEvent => playSoundAt(so, thisEvent.value)
        }
    })*/

  }
}


object OpenALComponent {

  /*constructor params*/
  val soundConfigFile = types.String
  // val soundEvents = Ontology.listOfVector3Events as SemanticSymbols.soundEvents
  val soundEvent = types.EventDescription as SoundSymbols.soundObjects



  /*entity creation params*/
  val onCreationParam = types.Container as SoundSymbols.onCreation
  val onCollisionParam = types.Container as SoundSymbols.onCollision
  val onEventParam = types.Container as SoundSymbols.onEvent

  // val events = Ontology.events

  object onEvent {
    val events = types.EventDescription

  }

  object onCollision {
    val play = types.Boolean
    val soundMaterial = types.Identifier as Symbols.material
  }

  object soundObject{
    //attributes for SoundObject
    val soundFile = types.String as Symbols.file
    val looping = types.Boolean

    val shared = types.Integer
    val maxNoOfChannels = types.Integer
  }

  def water(i: Int) =  types.Vector3 as SoundSymbols.water(i)

  val explosion = types.Vector3 as SoundSymbols.explosion
  val hitByFlame = types.Vector3 as SoundSymbols.hitByFlame
  val playerHit = types.Vector3 as SoundSymbols.playerHit
  val playerStartMoving = types.Vector3 as SoundSymbols.playerStartMoving
  val playerStopMoving = types.Vector3 as SoundSymbols.playerStopMoving
  val playerJumped = types.Vector3 as SoundSymbols.playerJumped
  val hitByIce = types.Vector3 as SoundSymbols.hitByIce
  val shieldEnabled = types.Vector3 as SoundSymbols.shieldEnabled
  val shieldDisabled = types.Vector3 as SoundSymbols.shieldDisabled
  val wellApproached = types.Vector3 as SoundSymbols.wellApproached
  val wellLeft = types.Vector3 as SoundSymbols.wellLeft
  val notEnoughMana = types.Vector3 as SoundSymbols.notEnoughMana
  val beginIceCharge = types.Vector3 as SoundSymbols.beginIceCharge
  val endIceCharge = types.Vector3 as SoundSymbols.endIceCharge
  val beginFireCharge = types.Vector3 as SoundSymbols.beginFireCharge
  val endFireCharge = types.Vector3 as SoundSymbols.endFireCharge
  val alarm = types.Vector3 as SoundSymbols.alarm
  val playerWon = types.Vector3 as SoundSymbols.playerWon
  val playerLost = types.Vector3 as SoundSymbols.playerLost



  object Event{
    def water(i: Int) = new EventDescription(SoundSymbols.water(i))
    val explosion = new EventDescription(SoundSymbols.explosion)
    val hitByFlame = new EventDescription (SoundSymbols.hitByFlame)
    val hitByIce = new EventDescription (SoundSymbols.hitByIce)
    val shieldEnabled = new EventDescription (SoundSymbols.shieldEnabled)
    val shieldDisabled = new EventDescription (SoundSymbols.shieldDisabled)
    val playerHit = new EventDescription (SoundSymbols.playerHit)
    val playerStartMoving = new EventDescription (SoundSymbols.playerStartMoving)
    val playerStopMoving = new EventDescription (SoundSymbols.playerStopMoving)
    val playerJumped = new EventDescription (SoundSymbols.playerJumped)
    val wellApproached = new EventDescription (SoundSymbols.wellApproached)
    val wellLeft = new EventDescription (SoundSymbols.wellLeft)
    val notEnoughMana = new EventDescription (SoundSymbols.notEnoughMana)
    val beginIceCharge =  new EventDescription (SoundSymbols.beginIceCharge)
    val endIceCharge = new EventDescription (SoundSymbols.endIceCharge)
    val beginFireCharge =  new EventDescription (SoundSymbols.beginFireCharge)
    val endFireCharge = new EventDescription (SoundSymbols.endFireCharge)
    val alarm = new EventDescription (SoundSymbols.alarm)
    val playerWon =  new EventDescription (SoundSymbols.playerWon)
    val playerLost = new EventDescription (SoundSymbols.playerLost)
  }
}
