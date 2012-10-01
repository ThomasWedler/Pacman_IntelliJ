package siris.components.network

/**
 *  Container class for multiple commands.
 */
private [network] class Packet extends Serializable{
  var sender : Participant = null
  var receiver : Participant = null
  var commands : List[Command] = List()
}