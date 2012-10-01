package siris.components.tuio

import siris.core.component.Component
import siris.core.helper.TimeMeasurement
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.entity.Entity
import siris.core.entity.description.{SValList, EntityAspect}
import siris.core.entity.component.{Removability, EntityConfigLayer}
import siris.ontology.Symbols
import siris.components.eventhandling.EventProvider
import TUIO._
import siris.core.svaractor.{SVarActorImpl, SVarActorLW}
import collection.mutable
import simplex3d.math.floatm.Vec2f
import simplex3d.math.floatm.FloatMath.length
//import siris.applications.fishtank.GlobalCfg

//Gloabl types
import siris.ontology.{types => gt}

/*
* Created by IntelliJ IDEA.
* User: martin
* Date: 9/26/11
* Time: 11:50 AM
*/


/**
 * A simple tuio component that listens to tuio events on a specific port and
 * emits them as siris events.
 */
class TuioComponent(
  override val componentName : Symbol,
  port: Int = 3333)
  extends SVarActorLW with Component with TimeMeasurement with EntityConfigLayer with EventProvider {

  def componentType = Symbols.tuio

  private val self = this

  private case class ObjectData(pos: Vec2f, lastUpdated: Long, var removable: Boolean)

  private val cursors = mutable.Map[Int, Vec2f]()
  private val objects = mutable.Map[Int, ObjectData]()
//  private val objectTimeout = GlobalCfg.fiducialTimeoutInMillis * 1000L * 1000L
//  private val minCurDistToObjs = GlobalCfg.minCurDistToObjs
  private var hasChanged = false

  private case class RemoveCursor(tcur: TuioCursor)
  private case class AddCursor(tcur: TuioCursor)
  private case class UpdateCursor(tcur: TuioCursor)
  private case class Refresh()
  private case class AddObject(tobj: TuioObject)
  private case class UpdateObject(tobj: TuioObject)
  private case class RemoveObject(tobj: TuioObject)

  addHandler[AddCursor]{case msg =>
    cursors += (msg.tcur.getCursorID -> Vec2f(msg.tcur.getX, msg.tcur.getY))
  }
  addHandler[UpdateCursor]{case msg =>
    cursors.update(msg.tcur.getCursorID, Vec2f(msg.tcur.getX, msg.tcur.getY))
  }
  addHandler[RemoveCursor]{case msg =>
    cursors.remove(msg.tcur.getCursorID)
  }

//  private def cleanObjects() {
//    val obIds = objects.keys.toList
//    val timeLimit = System.nanoTime() - objectTimeout
//    obIds.foreach(id => {
//      if((objects(id).lastUpdated < timeLimit) && objects(id).removable) objects.remove(id)
//    })
//  }

//  private def isNearObject(pos: Vec2f): Boolean =
//    objects.find(obj => {length(obj._2.pos - pos) < minCurDistToObjs}).isDefined


  addHandler[Refresh]{case msg =>
    //println("Refreshing with " + cursors.size + " cursors --- " + objects.size + " objects")
    //cleanObjects()
    emitEvent(Events.interactions.createEvent(gt.Interactions(cursors.values.toList)))
  }
  addHandler[AddObject]{case msg =>
    objects += (msg.tobj.getSymbolID -> ObjectData(Vec2f(msg.tobj.getX, msg.tobj.getY), System.nanoTime(), false))
    emitEvent(
      Events.tuioObjectAdded.createEvent(
        gt.Identifier(Symbol(msg.tobj.getSymbolID.toString)),
        gt.Position2D(Vec2f(msg.tobj.getX, msg.tobj.getY)),
        gt.Angle(msg.tobj.getAngleDegrees)
      )
    )
  }
  addHandler[UpdateObject]{case msg =>
    objects += (msg.tobj.getSymbolID -> ObjectData(Vec2f(msg.tobj.getX, msg.tobj.getY), System.nanoTime(), false))
    emitEvent(
      Events.tuioObjectUpdated.createEvent(
        gt.Identifier(Symbol(msg.tobj.getSymbolID.toString)),
        gt.Position2D(Vec2f(msg.tobj.getX, msg.tobj.getY)),
        gt.Angle(msg.tobj.getAngleDegrees)
      )
    )
  }
  addHandler[RemoveObject]{case msg =>
    objects(msg.tobj.getSymbolID).removable = true
    emitEvent(
      Events.tuioObjectRemoved.createEvent(
        gt.Identifier(Symbol(msg.tobj.getSymbolID.toString)),
        gt.Position2D(Vec2f(msg.tobj.getX, msg.tobj.getY)),
        gt.Angle(msg.tobj.getAngleDegrees)
      )
    )
  }

  val listener = new TuioListener {
    /**
     *  this method is called after each bundle,
     *  use it to repaint your screen for example
     */
    def refresh(ftime: TuioTime) {self ! Refresh()}

    /**
     * a cursor was removed from the table
     */
    def removeTuioCursor(tcur: TuioCursor) {
      self ! RemoveCursor(tcur)
    }

    /**
     * a cursor was moving on the table surface
     */
    def updateTuioCursor(tcur: TuioCursor) {
      self ! UpdateCursor(tcur)
    }

    /**
     * this is called when a new cursor is detected
     */
    def addTuioCursor(tcur: TuioCursor) {
      self ! AddCursor(tcur)
    }

    /**
     * an object was removed from the table
     */
    def removeTuioObject(tobj: TuioObject) {
      self ! RemoveObject(tobj)
    }

    /**
     * an object was moved on the table surface
     */
    def updateTuioObject(tobj: TuioObject) {
      self ! UpdateObject(tobj)
    }

    /**
     * this is called when an object becomes visible
     */
    def addTuioObject(tobj: TuioObject) {
      self ! AddObject(tobj)
    }
  }

    //Tell the Worldinterface that interactions will be provided.
  provideEvent(Events.interactions)
  provideEvent(Events.tuioObjectAdded)
  provideEvent(Events.tuioObjectUpdated)
  provideEvent(Events.tuioObjectRemoved)

  val client = new TuioClient(port)
  client.addTuioListener(listener)
  client.connect()

  protected def configure(params: SValList) = {}
  protected def entityConfigComplete(e: Entity with Removability, aspect: EntityAspect) = {}
  protected def requestInitialValues(
    toProvide: Set[ConvertibleTrait[_]],
    aspect: EntityAspect,
    e: Entity,
    given: SValList) = {}
  protected def removeFromLocalRep(e: Entity) = {}
}