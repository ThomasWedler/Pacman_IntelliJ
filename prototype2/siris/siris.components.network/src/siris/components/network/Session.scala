package siris.components.network

/**
 */
class Session extends Serializable{
  var participants : List[Participant] = Nil
  var host : Participant = null
}
