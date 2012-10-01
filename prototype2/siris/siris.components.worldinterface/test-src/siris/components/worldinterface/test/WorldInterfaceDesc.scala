package siris.components.worldinterface.test

import actors.Actor
import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import siris.core.component.Component
import siris.core.svaractor.SVarActorHW
import siris.components.worldinterface.{WorldInterfaceEvent, WorldInterface}
import siris.components.eventhandling._
import siris.core.entity.description.{SValList, Semantics}
import siris.core.entity.Entity
import siris.ontology.types

/* author: dwiebusch
 * date: 17.09.2010
 */

// TODO Codes does not compile any more. please fix it!

class WorldInterfaceDesc extends Spec with ShouldMatchers {
  val eventHandler = new SVarActorHW with Component with EventHandler{

    protected def removeFromLocalRep(e: Entity)     {}
    protected def configure(params: SValList) {}

    def componentType = new Semantics { def toSymbol = 'handler }

    def handleEvent(e: Event) {}

    def componentName = 'handler
    val parent = Actor.self

    addHandler[WorldInterfaceEvent]{
      case WorldInterfaceEvent('testEvent, v) => parent ! v
      case WorldInterfaceEvent('testTrigger, (svar, entity, _)) => parent ! 'testTrigger
    }
  }

  val eventProvider = new SVarActorHW with Component with EventProvider{
    def componentType = new Semantics { def toSymbol = 'provider }
    def componentName = 'provider
    val parent = Actor.self



    //overrides old handler for testing reasons
    addHandler[RegisterEventHandlerMessage]{ msg =>
        requireEvent( msg.handler, msg.event )
        parent ! msg.event
    }

    protected def configure(params: SValList) = null

    protected def removeFromLocalRep(e: Entity) {}
  }

  describe("Registration: "){
    eventHandler.start()
    eventProvider.start()

    it("Registering components"){
      WorldInterface.registerComponent(eventHandler)
      WorldInterface.registerComponent(eventProvider)
      val a = WorldInterface.lookupComponent(eventHandler.componentName).get == eventHandler
      val b = WorldInterface.lookupComponent(eventProvider.componentName).get == eventProvider
      assert( a && b )
    }
    it ("Registering actors"){
      WorldInterface.registerActor('eventIn, eventHandler)
      WorldInterface.registerActor('eventOut, eventProvider)
      val a = WorldInterface.lookupActor('eventIn).get == eventHandler
      val b = WorldInterface.lookupActor('eventOut).get == eventProvider
      assert( a && b )
    }
    it ("Register entities"){
      val e = new Entity
      val e2= new Entity
      WorldInterface.registerEntity('testEntity, e)
      WorldInterface.registerEntity('testEntity2, e2)
      assert(WorldInterface.lookupEntity('testEntity).get == e && WorldInterface.lookupEntity('testEntity2).get == e2)
    }
    it("Create and Register entities"){
      WorldInterface.createEntity('testEntity3)
      assert( WorldInterface.lookupEntity('testEntity3).isDefined )
    }
    it("Create State Values in registered entities"){
      WorldInterface.createStateValue(types.Boolean, true, 'testEntity)
      val svar = WorldInterface.lookupStateValue(types.Boolean, 'testEntity)
      svar  match {
        case Some(sVar) => assert(WorldInterface.readSVarBlocking(sVar))
        case None => assert(false)
      }
    }
    it("Create State Values in unregistered entities"){
      WorldInterface.createStateValue(types.Boolean, true, 'testEntity4)
      assert(WorldInterface.readSVar(types.Boolean, 'testEntity4) == Some(true) )
    }
    it("Receive registered actors list"){
      assert( WorldInterface.getActorList.forall( p => p == 'eventIn || p == 'eventOut) )
    }
    it("Setting values of registered SVars"){
      WorldInterface.setStateValue(types.Boolean, 'testEntity4, false)
      assert(WorldInterface.readSVar(types.Boolean, 'testEntity4) == Some(false))
    }
  }
  describe("Services:"){
    it("Forward messages"){
      WorldInterface.registerActor('me, Actor.self)
      WorldInterface.forwardMessage('me, "Hallo")
      Actor.self.receive{
        case "Hallo" => assert(true)
        case _ => assert(false)
      }
    }
  }
  describe("Eventhandling:"){
    it("Providing and requiring events"){
      val tEvent = new Semantics{ def toSymbol = 'testEvent }
      eventHandler.requestEvent( new EventDescription(tEvent) )
      eventProvider.provideEvent( new EventDescription(tEvent) )
      Actor.self.receive{
        case e : Event => 
      }
      eventProvider.emitEvent(new WorldInterfaceEvent('testEvent, "testValue"))
      var testResult = false
      Actor.self.receiveWithin(1000){
        case "testValue" => testResult = true
      }
      assert(testResult)
    }
    it ("Register for ValueChange Events"){
      val svar = WorldInterface.lookupEntity('testEntity4).get.get(types.Boolean).get
      WorldInterface.addValueChangeTrigger(svar, 'testEntity4, 'testTrigger)
      val testTrigger = new Semantics { def toSymbol = 'testTrigger }
      WorldInterface.requireEvent(eventHandler, new EventDescription(testTrigger))
      svar.set(true)
      var testResult1 = false
      var testResult2 = false
      Actor.self.receiveWithin(1000){
        case 'testTrigger => testResult1 = true
      }
      WorldInterface.addValueChangeTrigger(types.Boolean, 'testEntity4, 'testTrigger)
      WorldInterface.setStateValue(types.Boolean, 'testEntity4, false)
      Actor.self.receiveWithin(1000){
        case 'testTrigger => testResult2 = true
      }
      assert(testResult1 && testResult2)
    }
  }
  describe("--- Done ---"){
    it(""){
      eventHandler.shutdown()
      eventProvider.shutdown()
      WorldInterface.shutdown()
    }
  }
}