/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/28/11
 * Time: 3:04 PM
 */
package siris.components.remote

import siris.core.component.{ConfigureComponentMessage, Component}
import siris.core.svaractor.{SVar, SVarActorHW}
import siris.core.entity.Entity

import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.remote.Node
import actors.{AbstractActor, Exit}
import actors.Actor
import siris.components.eventhandling.{EventDescription, Event, EventProvider, EventHandler}
import collection.mutable.Set
import siris.core.entity.description.{SValList, Semantics, NamedSValList}
import siris.ontology.{Symbols, types}
import scala.Some
import siris.core.helper.TimeMeasurement

//Global Types
import siris.ontology.{Symbols, types => gt}

object RemoteEventPassingComponent {

//  val localPort = types.Integer as SemanticSymbols.localPort
//  val remotePort = types.Integer as SemanticSymbols.remotePort
//  val remoteIp = types.String as SemanticSymbols.ip
//  val eventsToForward = Ontology.events as SemanticSymbols.eventsToForward
//  val localName = types.Identifier as SemanticSymbols.localName
//  val remoteName = types.Identifier as SemanticSymbols.remoteName
}

case class EmitRemoteEvent(event: Event)
case class ProvideRemoteEvent(eventDesc: EventDescription)
case class AreYouThere()
case class YesIam()

class RemoteEventPassingComponent (override val componentName : Symbol)
        extends SVarActorHW with Component with EventProvider with EventHandler with TimeMeasurement {

  def componentType = Symbols.network

  //Internal Classes
  private case class Configuration() {
    var name = "Unnamed REPC (" + this.hashCode().toString + ")"
    var localPort: Option[Int] = None
    var peer: Option[Node] = None
    var localName: Option[Symbol] = None
    var remoteName: Option[Symbol] = None
    var eventsToForward: List[EventDescription] = Nil
  }

  private case class TryToConnect()
  private case class Connected()


  override def WakeUpMessage: Any = TryToConnect()
  //Internal Classes END

  //Members
  private val conf = Configuration()
  private var remoteActor: Option[AbstractActor] = None
  private var connected = false
//  private val forwardEventHandlers = Set[PartialFunction[Event, Unit]]()

  override def startUp() = {}

  //Register the handlers
  protected def configure(params: SValList) = {

    params.getAllCreateParamsFor(gt.NamedContainer).foreach(ncSVal => {
      ncSVal.value match {
        case nc@NamedSValList(Symbols.local, _) =>
          conf.localName = nc.getFirstValueFor(gt.Identifier)
          conf.localPort = nc.getFirstValueFor(gt.Port)
      }
      ncSVal.value match {
        case nc@NamedSValList(Symbols.remote, _) =>
          conf.remoteName = nc.getFirstValueFor(gt.Identifier)
          nc.getFirstValueFor(gt.Ip).collect{ case ip =>
              nc.getFirstValueFor(gt.Port).collect{ case port =>
                conf.peer = Some(Node(ip, port)) }}
      }
    })

    params.getAllCreateParamsFor(gt.EventDescription).map(_.value).foreach(eventDesc =>
      conf.eventsToForward = eventDesc :: conf.eventsToForward)

    //Initialize connection
    if (conf.peer.isDefined && conf.localPort.isDefined && conf.localName.isDefined && conf.remoteName.isDefined) {
      alive(conf.localPort.get)
      register(conf.localName.get, Actor.self)

      this ! TryToConnect()
    }
    else {
      println("RemoteEventPassingComponent configuration did not contain all required Params.")
      println("Forwarding to remote actor will not work.")
    }
  }

  addHandler[TryToConnect]{ msg: TryToConnect =>
    if(!connected) {
      RemoteActor.classLoader = getClass().getClassLoader()

      remoteActor.collect{case a => unlink(a)}
      remoteActor = Option(select(conf.peer.get, conf.remoteName.get))
      link(remoteActor.get)

      remoteActor.get ! AreYouThere()

      requestWakeUpCall(Period(1000))
      //new WakeUpActor(1000, this).start
    }
  }

  addHandler[AreYouThere]{ msg: AreYouThere =>
    remoteActor.collect{ case a => a ! YesIam() }
  }

  addHandler[YesIam]{ msg: YesIam =>
    if(!connected){
      connected = true
      this ! Connected()
    }
  }

  addHandler[Connected]{ msg: Connected =>
    println(conf.name + " successfully connected to " + conf.remoteName.get.name + "@"+ conf.peer.get.address + ":" + conf.peer.get.port)
    conf.eventsToForward.foreach(addEventForwarding(_))
  }

  def addEventForwarding[T](eventDesc: EventDescription) {
    requestEvent(eventDesc)
    remoteActor.collect({ case ra: AbstractActor => ra ! ProvideRemoteEvent(eventDesc)})
  }

  def handleEvent(e: Event) = {
    remoteActor.collect({ case ra: AbstractActor => ra ! EmitRemoteEvent(e)})
  }

  addHandler[ProvideRemoteEvent]{ msg: ProvideRemoteEvent =>
    provideEvent(msg.eventDesc)
  }

  addHandler[EmitRemoteEvent]{ msg: EmitRemoteEvent =>
    emitEvent(msg.event)
  }

  //TODO
  protected def entityConfigComplete(e: Entity, cParam: NamedSValList) = {}

  //TODO
  protected def handleNewSVar[T](sVarName: Symbol, sVar: SVar[T], e: Entity, cparam: NamedSValList) = {}

  //TODO
  protected def removeFromLocalRep(e: Entity) = {}
}