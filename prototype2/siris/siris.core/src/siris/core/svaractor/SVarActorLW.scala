package siris.core.svaractor

import synclayer.{SVarSyncLayer, PluggableSyncLayer}

/* author: dwiebusch
 * date: 17.09.2010
 */


object SVarActorLW extends SVarActorObjectInterface {
  def self: SVarActorImpl = SVarActorImpl.self

  def actor(initF: => Unit) : SVarActor with SVarSyncLayer = {
    val a = new SVarActorLW with PluggableSyncLayer{
      override def startUp() { initF }
    }
    a.start()
    a
  }
}

abstract class SVarActorLW extends SVarActorImpl {
  final def act() {
    startUp()
    loopWhile(isRunning) {
      react(neverMatchingHandler)
    }
  }
}
