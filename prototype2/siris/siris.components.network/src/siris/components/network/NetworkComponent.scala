package siris.components.network

import siris.core.component.Component
import siris.core.entity._
import description._
import siris.core.svaractor.{SVar, SVarActorHW}
import siris.core.svaractor.SVar
import actors.Actor
import simplex3d.math.floatm.renamed._
import simplex3d.math.floatm.FloatMath._
import simplex3d.math.floatm._
import siris.core.entity.component.Removability
import java.net.InetSocketAddress
import siris.components.network._
import siris.ontology.Symbols

/**
 *  Component handling the high level network functionality.
 * @param confParam         Collection of create parameters (currently not used).
 * @param componentName     Symbol used to represent this component
 */
class NetworkComponent(confParam: SValList, override val componentName : Symbol) extends SVarActorHW with Component {

  protected def configure(params: SValList) = null

  def componentType = Symbols.network
  def removeFromLocalRep(e: Entity) = {}

  private var svars : Map[Symbol, Tuple2[SVar[_], NamedSValList]] = Map()
  private var changes : List[Tuple2[Symbol, SVar[_]]] = List()

  var session : Session = null
  var participant : Participant = null

  private var networkIO : Actor = new IO (Actor.self)
  networkIO.start
  Actor.self ! Update( Actor.self )

  addHandler[Update] {
    case Update( sender ) => {
      // send messages for all changes, then throw them away
      session.participants.foreach(p => {
        if(p != participant){
          networkIO ! Send(participant, p, List(new UpdateCommand(changes)))
        }
      })
      changes = List()
      // make sure component stays updated
      Actor.self ! Update( Actor.self )
    }
  }

  addHandler[CreateSession] {
    case CreateSession ( port ) => {
      networkIO ! SetPort(port)
      participant = new Participant
      participant.address = new InetSocketAddress("localhost", port)
      session = new Session
      session.host = participant
    }
  }

  addHandler[JoinSession] {
    case JoinSession ( host, port ) => {
      networkIO ! SetPort(port)
      participant = new Participant
      participant.address = new InetSocketAddress("localhost", port)
      var receiver = new Participant
      receiver.address = new InetSocketAddress(host, port)
      networkIO ! Send(participant, receiver, List(new JoinCommand(participant)))
    }
  }

  /**
   *  Handler for all commands received over the network.
   */
  addHandler[Received] {
    case Received ( sender, receiver, commands ) => {
      commands.foreach(command => {
        command.cmdType match {
          // for all participants
          case CommandType.Update => {
            val cmd = command.asInstanceOf[UpdateCommand]
            //cmd.payload.foreach(p => {
            //  svars(p._1)._1.set(p._2.get)
            //})
          }

          // for host only
          case CommandType.Join => {
            val cmd = command.asInstanceOf[JoinCommand]
            if(session.participants.contains(cmd.participant))
            {
              networkIO ! Send(participant, cmd.participant, List(new RefusedCommand(cmd.participant, "participant is already in session")))
            } else {
              networkIO ! Send(participant, cmd.participant, List(new AcceptedCommand(cmd.participant)))
              session.participants = session.participants ::: cmd.participant :: Nil
              session.participants.foreach(p => {
                if(p != participant){
                  networkIO ! Send(participant, p, List(new JoinedCommand(cmd.participant, session)))
                }
              })
            }
          }
          case CommandType.Leave => {
            val cmd = command.asInstanceOf[LeaveCommand]
            if(session.participants.contains(cmd.participant))
            {
              session.participants = session.participants.filterNot(p => p == cmd.participant)
            }
            session.participants.foreach(p => {
                  networkIO ! Send(participant, p, List(new LeftCommand(cmd.participant, session)))
            })
          }

          // for participant
          case CommandType.Accepted => {
            val cmd = command.asInstanceOf[AcceptedCommand]
            // todo update local session object
            // start synchronization
          }
          case CommandType.Refused => {
            val cmd = command.asInstanceOf[RefusedCommand]
            println("host refused connection: " + cmd.reason)
          }
          case CommandType.Joined => {
            val cmd = command.asInstanceOf[JoinedCommand]
            session = cmd.session
          }
          case CommandType.Left => {
            val cmd = command.asInstanceOf[LeftCommand]
            session = cmd.session
          }

          case _ => {
            warn("Received command with type " + command.cmdType + " which is not handled.")
          }
        }
      })
    }
  }

  addHandler[Error] {
    case Error ( description ) => {
      // todo error handling, e.g. reconnect, switch port, retry
      warn(description)
    }
  }

  /**
   * @param e               Entity which is configured completly (all sVar's created).
   * @param cParam          Parameters used to create the entity (and all the sVars contained).
   */
  def entityConfigComplete( e: Entity, cParam: NamedSValList ) = {
    // empty
  }

  /**
   *  Called for each sVar that's created with a network aspect.
   * @param sVarName        Symbol used to identify the sVar.
   * @param sVar            Reference to the sVar-object.
   * @param e               Entity to which the sVar belongs.
   * @param cParam          Parameters used to create the sVar.
   */
  def handleNewSVar[T]( sVarName: Symbol, sVar: SVar[T], e: Entity, cParam: NamedSValList ) = {
    var aspectType: SVal[Semantics] = cParam.semantics
    var id = cParam.firstValueFor( Network.id );
    if(aspectType equals Symbols.netProduce){
      sVar.observe( observeHandler( sVarName, sVar, id, e, cParam, _ ) );
    }
    svars += id -> (sVar, cParam)
  }

  /**
   *  Handle added to observe changes in sVar's with a network aspect.
   * @param sVarName        Symbol used to identify the sVar.
   * @param sVar            Reference to the sVar-object.
   * @param id              Unique identifier (independent of the application instance) of this sVar.
   * @param e               Entity to which the sVar belongs.
   * @param cParam          Parameters used to create the sVar
   * @tparam value          Template type of the sVar.
   */
  def observeHandler[T]( sVarName: Symbol, sVar: SVar[T], id : Symbol, e: Entity, cParam : NamedSValList, value : T ) : Unit  = {
    changes = changes ::: (id, sVar) :: Nil
  }
}
