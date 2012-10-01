package siris.components.physics.simsim

import siris.core.helper.{TimeMeasurement, SVarUpdateFunctionMap}
import siris.components.eventhandling.Event
import siris.core.entity.Entity
import siris.ontology.types.{Transformation, Impulse, Velocity, Gravity}
import siris.core.entity.component.{Removability, EntityConfigLayer}
import siris.core.entity.typeconversion.ConvertibleTrait
import simplex3d.math.floatm.{Vec3f, Mat4x4f}
import siris.core.entity.description.{NamedSValList, EntityAspect, SValList, SVal}
import siris.ontology.{Symbols, types => gt}
import siris.components.physics.{Level, PhysicsException, PhysicsComponent}
import siris.core.svaractor.SVarActorHW
import collection.{immutable, mutable}

//Global Types
/*
* Created by IntelliJ IDEA.
* User: martin
* Date: 8/25/11
* Time: 11:51 AM
*/

/**
 * A simple physics component that can simulate particles that do not collide.
 */
class SimpleSimulator( override val componentName : Symbol )
  extends SVarActorHW with PhysicsComponent with SVarUpdateFunctionMap with TimeMeasurement with EntityConfigLayer{

  ignoredWriters = immutable.Set(this)

  private var gravity = Vec3f.Zero
  private var simulationSpeed = 1f
  private val particles = mutable.Map[Entity, Particle]()

  protected def configure(params: SValList) = {
    params.getFirstValueFor(gt.Gravity).collect{case g => gravity = g}
    params.getFirstValueFor(gt.SimulationSpeed).collect{case simSpeed => simulationSpeed = simSpeed}
  }

  override def startUp() {
    getDeltaT
    this ! WakeUp()
  }

  addHandler[WakeUp]{ msg =>
    val deltaT = getDeltaT.toSeconds
    //println("Simulating with a deltaT of " + deltaT + " sec.")
    startTimeMeasurement()
    simulate(deltaT * simulationSpeed)
    updateAllSVars
    requestWakeUpCall(timeToNextFrame())
  }

  private def simulate(deltaT: Float) {
    particles.values.foreach(_.simulate(deltaT))
  }

  override def getAdditionalProvidings( aspect : EntityAspect ) : Set[ConvertibleTrait[_]] = {
    val tcps = new NamedSValList(aspect.createParamSet)
    tcps.semantics match {
      case Symbols.particle =>
        Set[ConvertibleTrait[_]](gt.LinearDamping)
      case _ =>
        println(PhysicsException(tcps.semantics + " aspects are not supported by SimpleSimulator.", Level.warn))
        Set[ConvertibleTrait[_]]()
    }
  }

  protected def requestInitialValues(toProvide: Set[ConvertibleTrait[_]], aspect: EntityAspect, e: Entity, given: SValList) = {
    val tcps = new NamedSValList(aspect.createParamSet)

    tcps.semantics match {
      case Symbols.particle =>
        tcps.addIfNew(gt.Mass(1f))
        tcps.addIfNew(gt.Velocity(Vec3f.Zero))
        tcps.addIfNew(gt.Acceleration(Vec3f.Zero))
        provideInitialValues(e, tcps.combineWithValues(toProvide)._1)
      case _ =>
        println(PhysicsException(tcps.semantics + " aspects are not supported by SimpleSimulator.", Level.warn))
    }
  }

  //Todo: Collect svar values from e and use them to create the internal reps
  protected def entityConfigComplete(e: Entity with Removability, aspect: EntityAspect) = {

    val tcps = new NamedSValList(aspect.createParamSet)

    tcps.semantics match {
      case Symbols.particle =>
        val transformation = tcps.getFirstValueFor(gt.Transformation).getOrElse(
          throw PhysicsException("Reading the transformation from entity is not supported by SimpleSimulator."))
        val mass = tcps.getFirstValueForOrElse(gt.Mass)(1f)
        val linDamping = tcps.getFirstValueForOrElse(gt.LinearDamping)(1f)

        val p = new Particle(mass = mass, linearDamping = linDamping)
        p.setTransformation(transformation)

        addSVarUpdateFunctions(
          e.get(gt.Transformation).get,
          Some(p.setTransformation _),
          Some(p.getTransformation _))
        addSVarUpdateFunctions(
          e.get(gt.Velocity).get,
          Some(p.setLinearVelocity _),
          Some(p.getLinearVelocity _))
        addSVarUpdateFunctions(
          e.get(gt.Acceleration).get,
          Some(p.setLinearAcceleration _),
          Some(p.getLinearAcceleration _))
        addSVarUpdateFunctions(
          e.get(gt.LinearDamping).get,
          Some((ld: Float) => p.linearDamping = ld),
          Some({() => {p.linearDamping}}))
        addSVarUpdateFunctions(
          e.get(gt.Mass).get,
          Some((m: Float) => p.mass = m),
          Some({() => {p.mass}}))

        particles += (e -> p)

      case _ =>
        println(PhysicsException(tcps.semantics + " aspects are not supported by SimpleSimulator.", Level.warn))
    }
  }

  protected def removeFromLocalRep(e: Entity) = {
    e.getAllSVars.foreach{ x =>
      //Stop observing
      x._3.ignore()
      //Remove update function
      removeSVarUpdateFunctions( x._3 )
    }

    //Remove from local maps and simulation
    particles.remove(e)
  }

  def handleEvent(e: Event) = {}

  def handleSetTransformation(e: Entity, t: SVal[Transformation.dataType]) =
    throw PhysicsException("Not implemented.")
  def handleApplyImpulse(e: Entity, i: SVal[Impulse.dataType]) =
    throw PhysicsException("Not implemented.")
  def handleApplyTorqueImpulse(e: Entity, i: SVal[Impulse.dataType]) =
    throw PhysicsException("Not implemented.")
  def handleSetLinearVelocity(e: Entity, v: SVal[Velocity.dataType]) =
    throw PhysicsException("Not implemented.")
  def handleSetAngularVelocity(e: Entity, v: SVal[Velocity.dataType]) =
    throw PhysicsException("Not implemented.")
  def handleSetGravity(e: Entity, g: SVal[Gravity.dataType]) =
    throw PhysicsException("Not implemented.")
  def handleDetachEntity(e: Entity) =
    throw PhysicsException("Not implemented.")
  def handleAttachEntity(e: Entity) =
    throw PhysicsException("Not implemented.")
}