package siris.components.vrpn

import vrpn.{AnalogRemote, TextReceiver, ButtonRemote, TrackerRemote}
import siris.core.svaractor._
import siris.core.entity.Entity
import siris.core.component.Component
import siris.core.entity.description._
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.ontology.{Symbols, types}
import simplex3d.math.floatm.{Mat4f, Vec3f}
import simplex3d.math.floatm.renamed._
import java.lang.Exception
import siris.core.entity.component.{EntityConfigLayer, Removability}

/* author: dwiebusch
* date: 23.09.2010
*/

/*
 * To register new vrpn types, you have to take 2 steps here:
 * 1. create a handling function and add it to the createHandle function (as already done with the existing functions)
 * 2. enter the correct call to create for the new type in the VRPNActor.createOValue function
 *
 * --> don't forget to follow the steps which can be found in the SirisVRPN.scala file
 */
class VRPNConnector(val componentName : Symbol = 'vrpnconnector ) extends SVarActorHW with Component with EntityConfigLayer{
  def componentType = Symbols.vrpn
  //enable debugging output here
  val debug = true
  //the internal actor, which just updates its ovalues
  /**
   * standard full trackerinfo handling
   */
  protected def configure(params: SValList) {}

  def removeFromLocalRep(e: Entity) {}

  override def shutdown() {
    VRPNFactory.cleanup()
    super.shutdown()
  }

  def createHandle[T]( typ : Symbol, semantics : Symbol,  oval : SVar[T] ) : Option[ (Any, Any) => Unit ] = {
    typ match {
      case VRPN.position.sVarIdentifier => Some(handlePosition(semantics, oval) _ )
      case VRPN.oriAndPos.sVarIdentifier => Some(handleOriAndPos(semantics, oval) _ )
      case VRPN.orientation.sVarIdentifier => Some(handleOrientation(semantics, oval) _ )
      case VRPN.button.sVarIdentifier => Some(handleButton(semantics, oval) _ )
      case VRPN.text.sVarIdentifier => Some(handleText(semantics, oval) _ )
      case VRPN.analog.sVarIdentifier => Some(handleAnalog( semantics, oval) _ )
      case _ => None
    }
  }

  /**
   * standard full trackerinfo handling
   */
  def handlePosition[T](semantics : Symbol, oval : SVar[T])( msg : Any, src : Any){
    msg match{
      case msg : TrackerRemote#TrackerUpdate if (Symbol(msg.sensor.toString) == semantics) =>
        oval.set( createMat(msg.pos).asInstanceOf[T] )
      case msg : TrackerRemote#TrackerUpdate => {}
      case _ => println("ERROR: handlePosition got something that was no trackerupdate: " + msg)
    }
  }

  /**
   * standard full trackerinfo handling
   */
  def handleOrientation[T](semantics : Symbol, oval : SVar[T])( msg : Any, src : Any){
    msg match {
      case msg : TrackerRemote#TrackerUpdate if (Symbol(msg.sensor.toString) == semantics) =>
        oval.set( quat2Mat(msg.quat).asInstanceOf[T] )
      case msg : TrackerRemote#TrackerUpdate => {}
      case _ => println("ERROR: handleOrientation got something that was no trackerupdate: " + msg)
    }
  }

  /**
   * standard full trackerinfo handling
   */
  def handleOriAndPos[T](semantics : Symbol, oval : SVar[T])( msg : Any, src : Any){
    msg match {
      case msg : TrackerRemote#TrackerUpdate if (Symbol(msg.sensor.toString) == semantics) =>
        oval.set( Mat4 ( quatAndPos2Mat(msg.quat, msg.pos)).asInstanceOf[T] )
      case msg : TrackerRemote#TrackerUpdate => {}
      case _ => println("ERROR: handleOriAndPos got something that was no trackerupdate: " + msg)
    }
  }

  /**
   *  standard button press update handling
   */
  def handleButton[T](semantics : Symbol, oval : SVar[T])( msg : Any, src : Any){
    msg match{
      case msg : ButtonRemote#ButtonUpdate if (Symbol(msg.button.toString) == semantics) => {
        oval.set( (msg.state == 1).asInstanceOf[T] )
      }
      case msg : ButtonRemote#ButtonUpdate => {}
      case _ => println("ERROR: handleButton got something that was no button: " + msg)
    }
  }

  /**
   *  standard text update handling (untested)
   */
  def handleText[T](semantics : Symbol, oval : SVar[T])( msg : Any, src : Any){
    msg match {
      case msg : TextReceiver#TextMessage => oval.set( msg.msg.asInstanceOf[T] )
      case _ => println("ERROR: handleText got something that was no TextMessage")
    }
  }

  def handleAnalog[T](semantics : Symbol, oval : SVar[T] )(msg : Any, src : Any ) {
    msg match {
      case msg : AnalogRemote#AnalogUpdate =>
        oval.set( msg.channel( Integer.parseInt( semantics.toString().substring( 1, semantics.toString().length ) ) ).asInstanceOf[T] )
      case _ =>  println("ERROR: handleAnalog got something that was not an array of analog data")
    }
  }

  /**
   *  debug output
   */
  def handleInput( msg : Any, src : Any) {
    if (debug) println(msg)
  }

  /**
   * creates an position array
   */
  def createMat( arr : Array[Double] ) : Mat4 =
    Mat4( Mat3x4.translate( Vec3f(arr(0).toFloat, arr(1).toFloat, arr(2).toFloat) ) )
  /**
   *  creates a 4x4 matrix-representation of the given quaternion
   */
  def quat2Mat[A]( quat : Array[Double] ) : Mat4 = {
    val (qx , qy , qz , qw)  = (quat(0).toFloat, quat(1).toFloat, quat(2).toFloat, quat(3).toFloat)
    val (qx2, qy2, qz2, _) = (qx*qx, qy*qy, qz*qz, qw*qw)
    Mat4f(
      (1 - 2*qy2 - 2*qz2), (2*qx*qy - 2*qz*qw), (2*qx*qz + 2*qy*qw), 0,
      (2*qx*qy + 2*qz*qw), (1 - 2*qx2 - 2*qz2), (2*qy*qz - 2*qx*qw), 0,
      (2*qx*qz - 2*qy*qw), (2*qy*qz + 2*qx*qw), (1 - 2*qx2 - 2*qy2), 0,
      0,                   0,                   0,                   1
    )
  }
  /**
   * creates a combined matrix with quaternion and position information
   */
  def quatAndPos2Mat( quat : Array[Double], pos : Array[Double]) : Mat4 = {
    val (qx , qy , qz , qw)  = (quat(0).toFloat, quat(1).toFloat, quat(2).toFloat, quat(3).toFloat)
    val (qx2, qy2, qz2, _) = (qx*qx, qy*qy, qz*qz, qw*qw)
    Mat4f(
      (1 - 2*qy2 - 2*qz2), (2*qx*qy - 2*qz*qw), (2*qx*qz + 2*qy*qw), 0,
      (2*qx*qy + 2*qz*qw), (1 - 2*qx2 - 2*qz2), (2*qy*qz - 2*qx*qw), 0,
      (2*qx*qz - 2*qy*qw), (2*qy*qz + 2*qx*qw), (1 - 2*qx2 - 2*qy2), 0,
      pos(0).toFloat,      pos(1).toFloat,      pos(2).toFloat,      1
    )
  }

  override protected def entityConfigComplete(e: Entity with Removability, aspect: EntityAspect) {
    aspect.createParamSet.semantics match {
      case Symbols.trackingTarget =>
        connectSVar(VRPN.oriAndPos, e, aspect)
      case Symbols.button =>
        aspect.createParamSet.find(_.typedSemantics.getBase == types.Boolean.getBase).collect{
          case key => connectSVar(key.typedSemantics, e, aspect, Some(Symbols.button.value.toSymbol))
        }
      case something =>
        println("VRPN: unsupported parameter " + something.toString)
    }
  }

  override protected def requestInitialValues(toProvide: Set[ConvertibleTrait[_]], aspect: EntityAspect,
                                          e: Entity, given: SValList) {
    val (retVal, remaining) = aspect.createParamSet.combineWithValues(toProvide)
    provideInitialValues(e, aspect.createParamSet.semantics match {
      case Symbols.button => retVal
      case Symbols.trackingTarget => remaining.foldLeft(retVal){
        (set, elem) => elem match {
          case VRPN.oriAndPos => set += VRPN.oriAndPos(VRPN.oriAndPos.defaultValue())
          case something      => set
        }
      }
    })
  }

  private def connectSVar[T]( c : ConvertibleTrait[T], e : Entity, aspect : EntityAspect,
                              typ : Option[Symbol] = None, id : Option[Symbol] = None) {
    val url = aspect.createParamSet.getFirstValueFor(VRPN.url).getOrElse(throw new Exception("url missing"))
    val sem = id.getOrElse(aspect.createParamSet.getFirstValueFor(VRPN.id).getOrElse(throw new Exception("sem missing")))
    val updateRate = aspect.createParamSet.getFirstValueForOrElse(VRPN.updateRateInMillis)(16L)
    val sVarIdentifier = typ.getOrElse(c.sVarIdentifier)
    e.execOnSVar(c){
      sVar => VRPNFactory.createClient(sVarIdentifier, url, updateRate).collect {
        //create handle and check if this step was successful
        case client : VRPNClient => createHandle(sVarIdentifier, sem, sVar).collect{
          //create listener and check if this step was successful
          case handle => VRPNFactory.createListener(sVarIdentifier, handle).collect {
            // subscribe
            case listener => client subscribe listener
          }.getOrElse(println("VRPN createFor: could not create handle for " + url))
        }.getOrElse(println("VRPN createFor: could not regiser listener for " + url))
      }.getOrElse(println("VRPN createFor: could not create client for " + url))
    }
  }

  /*
  * configures the connector. this is only necessary if you want debugging output, as the create for method will
  * create new clients and listeners id necessary (when you didn't create them via configure)
  *
  * @param confParamType a List of Tuples containing the clients data type and the server url
  */
  def configure(param: List[(Symbol, String)]) {
    var listener : Option[VRPNListener] = None
    for ( (listenerSymbol, url) <- param ){
      val client = VRPNFactory.createClient(listenerSymbol, url)
      if (client.isDefined){
        //handle input (just outputs the received message)
        listener = VRPNFactory.createListener(listenerSymbol, handleInput _ )
        if (listener.isDefined){
          client.get.subscribe(listener.get)
        } else println("VRPN configure: could not create listener for " + url)
      } else println("VRPN configure: could not create client for " + url)
    }
  }
}