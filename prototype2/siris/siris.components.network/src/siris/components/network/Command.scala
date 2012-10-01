package siris.components.network

import siris.core.svaractor.SVar

/**
 * Command types for communication over the network.
 */
private [network] object CommandType extends Enumeration with Serializable {
  val NotSet = Value("NotSet")

  /* Commands for the host. */
  val Join = Value("Join")
  val Leave = Value("Leave")

  /* Commands from the host. */
  val Accepted = Value("Accepted")
  val Refused = Value("Refused")
  val Joined = Value("Joined")
  val Left = Value("Left")

  /* Commands for host and participants. */
  val Update = Value("Update")
}

/**
 *  Base class for all commands.
 */
private [network] class Command extends Serializable {
  var cmdType : CommandType.Value = CommandType.NotSet
}

/**
 * @param participant       Participant that requests to join a session.
 */
private [network] class JoinCommand(val participant : Participant) extends Command with Serializable{
  cmdType = CommandType.Join
}

/**
 * @param participant       Participant that leaves a session.
 */
private [network] class LeaveCommand(val participant : Participant) extends Command with Serializable{
  cmdType = CommandType.Leave
}

/**
 * @param participant       Participant that isn't allowed to join the session.
 * @param reason            Brief description why the request was refused.
 */
private [network] class RefusedCommand(val participant : Participant, val reason : String) extends Command with Serializable{
  cmdType = CommandType.Refused
}

/**
 * @param participant       Participant that is allowed to join the session.
 */
private [network] class AcceptedCommand(val participant : Participant) extends Command with Serializable{
  cmdType = CommandType.Accepted
}

/**
 * @param participant       The participant that joined the session.
 * @param session           Updated session object.
 */
private [network] class JoinedCommand(val participant : Participant, val session : Session) extends Command with Serializable{
  cmdType = CommandType.Joined
}

/**
 * @param participant       The participant that left the session.
 * @param session           Updated session object.
 */
private [network] class LeftCommand(val participant : Participant, val session : Session) extends Command with Serializable{
  cmdType = CommandType.Left
}

/**
 * @param payload           List of changes since the last update.
 */
private [network] class UpdateCommand(val payload : List[Tuple2[Symbol, SVar[_]]]) extends Command with Serializable{
  cmdType = CommandType.Update
}
