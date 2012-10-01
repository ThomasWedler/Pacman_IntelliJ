package siris.components.network

import actors.Actor

/**
 * Messages used to communicate with the network component.
 */
// update the components internal state, the component starts updating itself after receiving one initial update message
case class Update( sender : Actor )
// host a new session
case class CreateSession ( port : Short)
// join an existing session
case class JoinSession ( host : String, port : Short)
// disconnect from current session
case class LeaveSession ()
// send message to all (of this session)
//case class Send ()

/**
 * Messages used to communicate between the component and the io-actor.
 */
// received data
private [network] case class Received ( sender : Participant, receiver : Participant, commands : List[Command] )
// send data
private [network] case class Send (  sender : Participant, receiver : Participant, commands : List[Command] )
// set the port
private [network] case class SetPort ( newPort : Short )
//
private [network] case class Error ( description : String )
