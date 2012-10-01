/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 3/2/11
 * Time: 3:30 PM
 */
package siris.components.remote

import siris.core.svaractor.{SVar, SVarActorHW}
import siris.core.entity.Entity
import siris.components.eventhandling._
import siris.components.worldinterface.WorldInterface
import siris.core.component.{ConfigureComponent, Component}
import siris.ontology.{Symbols, types}
import siris.core.entity.description.{SValList, Semantics, NamedSValList}
import siris.core.helper.TimeMeasurement
;
object RemoteEventPassingTest {
  def main( args : Array[String]) : Unit = {
    val command = if(args.size == 1) {
      if((args(0) == "ping") || (args(0) == "pong") || (args(0) == "local")) args(0) else "local"
    } else "local"

    if(command == "local") {
      println("Starting local test")
      (new PongComp).start
      (new PingComp).start
    }
    if(command == "ping") {
      println("Starting ping test")
      val repc = new RemoteEventPassingComponent('repc)
//      repc !
//        ConfigureComponent(
//          RemoteEventPassingComponent.localPort apply 9000,
//          RemoteEventPassingComponent.localName apply 'Ping,
//          RemoteEventPassingComponent.remoteIp apply "127.0.0.1",
//          RemoteEventPassingComponent.remotePort apply 9001,
//          RemoteEventPassingComponent.remoteName apply 'Pong,
//          RemoteEventPassingComponent.eventsToForward apply randomEventDesc :: Nil
//        )
      RemoteEventPassingConfiguration('Ping, 9000, 'Pong, "127.0.0.1", 9001, randomEventDesc).deliverTo(repc)
      repc.start
      (new PingComp).start
    }
    if(command == "pong") {
      println("Starting pong test")
      val repc = new RemoteEventPassingComponent('repc)
//      repc !
//        ConfigureComponent(
//          RemoteEventPassingComponent.localPort apply 9001,
//          RemoteEventPassingComponent.localName apply 'Pong,
//          RemoteEventPassingComponent.remoteIp apply "127.0.0.1",
//          RemoteEventPassingComponent.remotePort apply 9000,
//          RemoteEventPassingComponent.remoteName apply 'Ping
//        )
      RemoteEventPassingConfiguration('Pong, 9001, 'Ping, "127.0.0.1", 9000, randomEventDesc).deliverTo(repc)
      repc.start
      (new PongComp).start
    }
  }

  val random = types.Integer as Symbols.identifier
  val randomEventDesc = new EventDescription(Symbols.identifier)
  val pings = 512
}

class PingComp(override val componentName : Symbol = 'PingComp)
        extends SVarActorHW with Component with EventProvider with EventHandler with TimeMeasurement {

  protected def configure(params: SValList) = null

  def componentType = siris.ontology.types.OntologySymbol(componentName)

  private case class PingMsg()
  private var pingsLeft = 0

  provideEvent(RemoteEventPassingTest.randomEventDesc)

  override def startUp = {
    pingsLeft = RemoteEventPassingTest.pings
    this ! PingMsg()
  }

  addHandler[PingMsg]{ case msg: PingMsg =>
    pingsLeft -= 1
    if(pingsLeft == 0) {
      emitEvent(RemoteEventPassingTest.randomEventDesc(types.Integer(0)))
      shutdown
    }
    else {
      emitEvent(RemoteEventPassingTest.randomEventDesc.createEvent(types.Integer(scala.util.Random.nextInt)))
      scheduleExecution(250L) {
        this ! PingMsg()
      }
    }
  }

  protected def entityConfigComplete(e: Entity, cParam: NamedSValList) = {}
  protected def removeFromLocalRep(e: Entity) = {}
  protected def handleNewSVar[T](sVarName: Symbol, sVar: SVar[T], e: Entity, cparam: NamedSValList) = {}

  def handleEvent(e: Event) = {}
}

class PongComp(override val componentName : Symbol = 'PongComp)
        extends SVarActorHW with Component with EventProvider with EventHandler {

  protected def configure(params: SValList) = null

  def componentType =siris.ontology.types.OntologySymbol( componentName )

  requestEvent(RemoteEventPassingTest.randomEventDesc)

  override def startUp = {}

  protected def entityConfigComplete(e: Entity, cParam: NamedSValList) = {}
  protected def removeFromLocalRep(e: Entity) = {}
  protected def handleNewSVar[T](sVarName: Symbol, sVar: SVar[T], e: Entity, cparam: NamedSValList) = {}

  def handleEvent(e: Event) = {
    //val ev = RemoteEventPassingTest.randomEventDesc.upcast(e).collect({
      if (RemoteEventPassingTest.randomEventDesc.name == e.name){
        println("Event received: " + e.get(RemoteEventPassingTest.random))
        e get RemoteEventPassingTest.random collect{
          case 0 =>
            WorldInterface.shutdown
            shutdown
        }
    }
  }
}

