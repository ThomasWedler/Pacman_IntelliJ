package siris.components.physics.jbullet

import scala.collection.mutable
import scala.collection.immutable

import siris.core.entity.Entity
import siris.core.entity.description._
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.svaractor.synclayer.PluggableSyncLayer
import siris.core.svaractor.SVarActorHW
import siris.core.helper.{TimeMeasurement, SVarUpdateFunctionMap}

import siris.components.eventhandling._
import siris.ontology.Symbols

import javax.vecmath.Vector3f

import com.bulletphysics.collision.dispatch._
import com.bulletphysics.collision.broadphase._
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver
import com.bulletphysics.dynamics._
import siris.core.entity.component.{EntityConfigLayer, Removability}
import siris.components.physics.{Level, PhysicsException, PhysicsComponent, PhysicsEvents}

//Helps shortening the javax and jbullet.linearmath code
import siris.components.physics.jbullet.{JBulletMath => m, types => lt}
//Global Types
import siris.ontology.{types => gt}
//Local Types

/**
 * A component implementation for the the JBullet physics engine.
 *
 * @param componentName   The name to register the component with.
 * @param maxNrOfObjects  The maximum number of simulated objects
 * @param nrOfSubsteps    Number of substeps maximally made by the engine in one step
 * @param fixedTimeStep   The size of one substep in seconds (see http://www.bulletphysics.org/mediawiki-1.5.8/index.php/Stepping_The_World)
 */
class JBulletComponent( override val componentName : Symbol = JBullet.getComponentName, maxNrOfObjects: Int = 1000,
                        nrOfSubsteps: Int = 1000, fixedTimeStep: Float = 1.f / (60.f * 2.f))
        extends SVarActorHW with PhysicsComponent with SVarUpdateFunctionMap with PluggableSyncLayer with TimeMeasurement with EntityConfigLayer {
  //
  //  Init
  //
  /**
   * Internal JBullet objects
   */
  private val collisionConfiguration = new DefaultCollisionConfiguration()
  private val dispatcher = new CollisionDispatcher(collisionConfiguration)
  private val solver = new SequentialImpulseConstraintSolver()
  private val broadphase = new SimpleBroadphase(maxNrOfObjects)

  /**
   * The simulation object.
   */
  private val discreteDynWorld = new JBulletDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration)
  discreteDynWorld.setInternalTickCallback(new InternalCallBackClass, new Object())

  /**
   * Stores the internal represenation (JBulletRigidBody) of entities.
   */
  private var entityMap = mutable.Map[Entity, JBulletRigidBody]()

  /**
   * Stores the external represenation (Entity) of JBulletRigidBodies.
   */
  private var rigidBodyMap = mutable.Map[JBulletRigidBody, Entity]()

  /**
   * Stores detached entities
   */
  private var detachedEntities = mutable.Set[Entity]()

  /**
   * Stores held JBulletRigidBodies and thier original mass.
   */
  private var heldRigidBodiess = mutable.Map[JBulletRigidBody, Float]()

  /**
   * Stores the simulation speed multipier
   */
  private var simulationSpeed: Float = 1.0f

  /**
   * Use SVarUpdateFunctionMap to ignore own writes when observing svars
   */
  ignoredWriters = immutable.Set(this)

  /**
   * 1/frameFrequency of JBullet in nanoseconds. See TimeMeasurement trait.
   */
  framePeriod = 16L * 1000L * 1000L
  //
  //Init End
  //

  //
  //  Register convertibles
  //
  registerConvertibleHint(lt.Transformation)
  registerConvertibleHint(lt.Vector3)
  registerConvertibleHint(lt.Velocity)
  registerConvertibleHint(lt.Gravity)
  registerConvertibleHint(gt.SimulationSpeed)
  //
  //  Register convertibles End
  //

  //register converters
  JBulletConverters.register()

  //
  //  Register events
  //
  provideEvent( PhysicsEvents.collision )
  requestEvent( PhysicsEvents.impulse )
  requestEvent( PhysicsEvents.torqueImpulse )
  //
  //  Register events End
  //

  //
  //  Handlers
  //
  /**
   *  Simulates a frame.
   * @see   JBSimulateFrame
   */
  addHandler[JBSimulateFrame]{ msg =>
    val deltaT = getDeltaT.toSeconds
    //println("Simulating with a deltaT of " + deltaT + " sec.")
    startTimeMeasurement()
    rigidBodyMap.keys.foreach(_.simulate(deltaT))
    discreteDynWorld.stepSimulation(deltaT * simulationSpeed, nrOfSubsteps, fixedTimeStep)
    updateAllSVars
    push()
    requestWakeUpCall(timeToNextFrame())
  }

  /**
   *  Makes a JBulletRigidBody static.
   * @see   JBHold
   */
  addHandler[JBHold]{ msg =>
    entityMap.get(msg.entity) collect {
      case rb =>
        if(!heldRigidBodiess.contains(rb)) {
          heldRigidBodiess += rb -> rb.getInvMass
          rb.setMassProps(0, new Vector3f(0,0,0))
        }
    }
  }

  /**
   *  Releases a held JBulletRigidBody.
   * @see   JBHold, JBRelease
   */
  addHandler[JBRelease]{ msg =>
    entityMap.get(msg.entity) collect {
      case rb =>
        heldRigidBodiess.get(rb).collect {
          case inverseMass =>
            if(inverseMass == 0f){
              rb.setMassProps(0, new Vector3f(0,0,0))
            }
            else {
              val mass = 1f / inverseMass
              val tempVec = new Vector3f()
              rb.getCollisionShape.calculateLocalInertia(mass, tempVec)
              rb.setMassProps(mass, tempVec)
            }

            heldRigidBodiess.remove(rb)
        }
    }
  }
  //
  //   Handlers End
  //

  /**
   * Message that triggers next frame processing. See TimeMeasurement trait.
   */
  override def WakeUpMessage: Any = JBSimulateFrame()

  /**
   *    Starting up the component.
   * Executed after Actor.start and before the first message handling.
   */
  override def startUp() {
    getDeltaT
    this ! JBSimulateFrame()
  }

  /**
   *  Configures JBullet
   */
  protected def configure(params: SValList) {
    params.getFirstValueFor(gt.SimulationSpeed).collect{ case s => simulationSpeed = s}
    params.getFirstValueFor(lt.Gravity).collect{ case g => discreteDynWorld.setGravity(g) }
  }

  /**
   *  Handles a SetTransformation message.
   *
   * @see PhysicsComponent, PhysicsMessages
   */
  def handleSetTransformation(e: Entity, t: SVal[gt.Transformation.dataType]) {
    entityMap.get(e).collect{ case rb => rb.setGraphicsWorldTransform(t.as(lt.Transformation)) }
  }

  /**
   * Handles a ApplyImpulse message.
   *
   * @see PhysicsComponent, PhysicsMessages
   */
  def handleApplyImpulse(e: Entity, i: SVal[gt.Impulse.dataType]) {
    entityMap.get(e).collect { case rb =>
      if (!rb.isActive) rb.activate()
      rb.applyCentralImpulse(i.as(lt.Impulse))
    }
  }

  /**
   * Handles a ApplyTorqueImpulse message.
   *
   * @see PhysicsComponent, PhysicsMessages
   */
  def handleApplyTorqueImpulse(e: Entity, i: SVal[gt.Impulse.dataType]) {
    entityMap.get(e).collect { case rb =>
      if (!rb.isActive) rb.activate()
      rb.applyTorqueImpulse(i.as(lt.Impulse))
    }
  }

  /**
   * Handles a SetLinearVelocity message.
   *
   * @see PhysicsComponent, PhysicsMessages
   */
  def handleSetLinearVelocity(e: Entity, v: SVal[gt.Velocity.dataType]) {
    entityMap.get(e).collect{ case rb => rb.setLinearVelocity(v.as(lt.Velocity)) }
  }

  /**
   * Handles a SetAngularVelocity message.
   *
   * @see PhysicsComponent, PhysicsMessages
   */
  def handleSetAngularVelocity(e: Entity, v: SVal[gt.Velocity.dataType]) {
    entityMap.get(e).collect{ case rb => rb.setAngularVelocity(v.as(lt.Velocity)) }
  }

  /**
   * Handles a SetGravity message.
   *
   * @see PhysicsComponent, PhysicsMessages
   */
  def handleSetGravity(e: Entity, g: SVal[gt.Gravity.dataType]) {
    entityMap.get(e).collect{ case rb => rb.setGravity(g.as(lt.Gravity)) }
  }

  /**
   * Handles a DetachEntity message.
   *
   * @see PhysicsComponent, PhysicsMessages
   */
  def handleDetachEntity(e: Entity) {
    if (!detachedEntities.contains(e))
      entityMap.get(e) collect {
        case rb =>
          //Disable simulation
          discreteDynWorld.removeRigidBody(rb)
          //Disable updates
          e.getAllSVars.foreach( x => pauseUpdatesFor(x._3) )
          //Store entity
          detachedEntities.add(e)
      }
  }

  /**
   * Handles a AttachEntity message.
   *
   * @see PhysicsComponent, PhysicsMessages
   */
  def handleAttachEntity(e: Entity) {
    if (detachedEntities.contains(e))
      entityMap.get(e) collect {
        case rb =>
          //Set position
          e.get(lt.Transformation).collect {
            case t => t.get((value) => {
              //Enable simulation
              discreteDynWorld.addRigidBody(rb, rb.collisionGroup, rb.collidesWith)
              //Magic
              rb.setCollisionFlags(0)
              //Update position
              rb.setGraphicsWorldTransform(value)
              //Reset velocity
              rb.clearForces()
              rb.setLinearVelocity(m.ZeroVec)
              rb.setAngularVelocity(m.ZeroVec)
              //Enable updates
              e.getAllSVars.foreach( x => resumeUpdatesFor(x._3) )
              //Update internal data structure
              detachedEntities.remove(e)
            })
          }
      }
  }

  /**
   *  Used to react on collisions.
   */
  private class InternalCallBackClass extends InternalTickCallback {

    def internalTick(world: DynamicsWorld, timeStep: Float) {
      val numManifolds = world.getDispatcher.getNumManifolds
      for (i <- 0 until numManifolds) {
        val contactManifold = world.getDispatcher.getManifoldByIndexInternal(i)
        val numContacts = contactManifold.getNumContacts
        for (j <- 0 until numContacts) {
          val pt = contactManifold.getContactPoint(j)
          if (pt.getDistance < 0.f) {
            val rbA = JBulletRigidBody.upcast(contactManifold.getBody0.asInstanceOf[CollisionObject])
            val rbB = JBulletRigidBody.upcast(contactManifold.getBody1.asInstanceOf[CollisionObject])
            if(rbA.isActive || rbB.isActive) handleCollision(rbA, rbB)
          }
        }
      }
    }
  }

  /**
   *  The reaction on a collision.
   */
  private def handleCollision(rbA: JBulletRigidBody, rbB: JBulletRigidBody) =
    rigidBodyMap.get(rbA) collect {
      case entityA =>
        rigidBodyMap.get(rbB) collect {
          case entityB =>
            emitEvent(PhysicsEvents.collision.createEvent(Set(entityA, entityB)))
        }
    }

  /**
   *  Removes an entity from this JBullet component.
   */
  def removeFromLocalRep(e: Entity) {

    e.getAllSVars.foreach{ x =>
      //Stop observing
      x._3.ignore()
      //Remove update function
      removeSVarUpdateFunctions( x._3 )
    }

    //Remove from local maps and simulation
    entityMap.get(e).collect({
      case rb: JBulletRigidBody =>
        rigidBodyMap -= rb
        entityMap -= e
        detachedEntities -= e
        discreteDynWorld.removeRigidBody(rb)
    })

  }

  /**
   *  Returns the additional providings to a given aspect.
   */
  override def getAdditionalProvidings( aspect : EntityAspect ) : Set[ConvertibleTrait[_]] = {
    var providings = Set[ConvertibleTrait[_]]()
    val tcps = aspect.createParamSet

    tcps.semantics match {
      case Symbols.configuration =>
        providings = providings + gt.SimulationSpeed
        providings = providings + gt.Gravity
      //RigidBody
      case _ =>
        //PhysBaseProps
        if(tcps.containsCreateParam(gt.Transformation)) providings = providings + gt.Transformation
        providings = providings + gt.Mass
        providings = providings + gt.Gravity
        providings = providings + gt.Restitution
        providings = providings + gt.LinearDamping
        providings = providings + gt.AngularDamping
        providings = providings + gt.AngularFactor
        //Additional
        providings = providings + gt.Velocity
    }

    providings
  }

  /**
   *  Returns the dependencies JBullet has to create an entity with a given aspect.
   */
  override def requestInitialValues( toProvide : Set[ConvertibleTrait[_]], aspect : EntityAspect, e : Entity,
                                 given : SValList ) {
    val tcps = new NamedSValList(aspect.createParamSet)

    val initialValues = tcps.semantics match {
      case Symbols.configuration =>
        tcps.addIfNew(gt.SimulationSpeed(simulationSpeed))
        tcps.addIfNew(lt.Gravity(discreteDynWorld.getGravity()))
      //RigidBody
      case _ =>
        (JBulletRigidBody(tcps) match {
          case Some(rb) => rb.getProperties.addIfNew(lt.Gravity(discreteDynWorld.getGravity()))
          case None => JBulletRigidBody.getFallbackProperties }).addIfNew(lt.Velocity(m.ZeroVec))
    }

    //if(!initialValues.satisfies(toProvide)) throw PhysicsException("[jbullet][error] Debug")
    provideInitialValues(e, initialValues.combineWithValues(toProvide)._1)
  }

  /**
   *  Registers a JBulletRigidBody at the JBullet component.
   */
  private def registerRigidBody(rb: JBulletRigidBody, e: Entity) = {
    discreteDynWorld.addRigidBody(rb, rb.collisionGroup, rb.collidesWith)
    entityMap += e -> rb
    rigidBodyMap += rb -> e
  }

  /**
   *  Connects a JBulletRigidBody and its corresponding SVars using the SVarUpdateFunctions trait.
   */
  private def connectSVars(rb: JBulletRigidBody, e: Entity) {
    try {
      updateSVarUpdateFunctions(e.get(lt.Transformation).get, rb.setGraphicsWorldTransform _, rb.getGraphicsWorldTransform.get _)
      updateSVarUpdateFunctions(e.get(gt.Mass).get, rb.setMass _, rb.getMass _)
      updateSVarUpdateFunctions(e.get(lt.Gravity).get, rb.setGravity _, rb.getGravity _)
      updateSVarUpdateFunctions(e.get(gt.Restitution).get, rb.setRestitution _, rb.getRestitution _)
      updateSVarUpdateFunctions(e.get(gt.LinearDamping).get, rb.setLinearDamping _, rb.getLinearDamping _)
      updateSVarUpdateFunctions(e.get(gt.AngularDamping).get, rb.setAngularDamping _, rb.getAngularDamping _)
      updateSVarUpdateFunctions(e.get(gt.AngularFactor).get, rb.setAngularFactor _, rb.getAngularFactor _)

      addSVarUpdateFunctions(e.get(lt.Velocity).get, (v: Vector3f)  => rb.setLinearVelocity(v), rb.getLinearVelocity)
      addSVarUpdateFunctions(e.get(lt.Acceleration).get, Some(rb.setLinearAcceleration _), Some(rb.getLinearAcceleration _))

    } catch {
      case e: NoSuchElementException => throw PhysicsException("Entity did not contain the assumed SVars.")
      case exception => throw PhysicsException("Error during entity creation.")
    }
  }

  /**
   *  Creates a JBulletRigidBody, connects it to its corresponding SVars and registers it.
   */
  private def createRigidBodyEntity(e: Entity, c : NamedSValList) {
    try {

      def create(collectedValues: SValList)  {
        JBulletRigidBody(new NamedSValList(c.semantics, collectedValues).xMergeWith(c)) match {
          case Some(rb) =>
            registerRigidBody(rb, e)
            connectSVars(rb, e)
          case None => println(PhysicsException("Error during rigid body construction. Ingnoring entity!", Level.warn))
        }
      }

      collectSVars(e, lt.Transformation, gt.Mass, lt.Gravity, gt.Restitution,
        gt.LinearDamping, gt.AngularDamping, gt.AngularFactor)(create, true)
    }
    catch {
      case e: NoSuchElementException => throw PhysicsException("Entity did not contain the assumed SVars.")
      case exception => throw PhysicsException("Error during entity creation.")
    }
  }

  /**
   *  Creates and connects the configuration entity.
   */
  private def createConfigurationEntity(e: Entity, c : NamedSValList) {
      c.getFirstValueFor(gt.SimulationSpeed).collect{case s => simulationSpeed = s}
      c.getFirstValueFor(lt.Gravity).collect{case g => discreteDynWorld.setGravity(g)}

      addSVarUpdateFunctions(e.get(lt.Gravity).get, Some((g: Vector3f) => {discreteDynWorld.setGravity(g)}), None)
      addSVarUpdateFunctions(e.get(gt.SimulationSpeed).get, Some((s: Float) => {simulationSpeed = s}), None)
  }

  override def entityConfigComplete(e : Entity with Removability, aspect : EntityAspect) {
    aspect.createParamSet.semantics match {
      case Symbols.configuration => createConfigurationEntity(e, aspect.createParamSet)
      //RigidBody
      case _ => createRigidBodyEntity(e, aspect.createParamSet)
    }
  }

  /**
   *  Handles incomming events
   */
  def handleEvent(e: Event) {
    //ApplyImpulse
    if (e.name.value.toSymbol == PhysicsEvents.impulse.name.value.toSymbol){
      e.affectedEntities.headOption.collect{ case entity =>
        entityMap.get(entity).collect{ case rb =>
          e.get(lt.Impulse).collect{ case impulse =>
            if(!rb.isActive) rb.activate()
            rb.applyCentralImpulse(impulse)
          }
        }
      }
    }
    //ApplyTorqueImpulse
    if (e.name.value.toSymbol == PhysicsEvents.torqueImpulse.name.value.toSymbol){
      e.affectedEntities.headOption.collect{ case entity =>
        entityMap.get(entity).collect{ case rb =>
          e.get(lt.Impulse).collect{ case impulse =>
            if(!rb.isActive) rb.activate()
            rb.applyTorqueImpulse(impulse)
          }
        }
      }
    }
  }
}

/**
 *  JBullets companion object. Includes helper functionality.
 */
object JBullet {

  private var count = -1

  private[jbullet] def getComponentName : Symbol = synchronized {
    count = count + 1
    Symbol(Symbols.physics.value.toSymbol.name + "#" + count)
  }
}