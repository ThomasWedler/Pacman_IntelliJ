package siris.components.network

import java.net.InetSocketAddress

/**
 *  Connects a participant name to a socket address.
 * @note The name is required if more than one participant is connected using the same socket address (for hot seat or split screen).
 */
class Participant extends Serializable{
  var name : String = ""
  var address : InetSocketAddress = null
}

