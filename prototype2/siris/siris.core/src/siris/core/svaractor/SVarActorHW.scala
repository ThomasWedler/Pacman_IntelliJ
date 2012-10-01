package siris.core.svaractor

import synclayer.{SVarSyncLayer, PluggableSyncLayer}

/* author: dwiebusch
 * date: 17.09.2010
 */

object SVarActorHW extends SVarActorObjectInterface {
  def self: SVarActorImpl = SVarActorImpl.self

  def actor(initF: => Unit) : SVarActor with SVarSyncLayer = {
    val a = new SVarActorHW with PluggableSyncLayer {
      override def startUp() { initF }
    }
    a.start()
    a
  }
}

abstract class SVarActorHW extends SVarActorImpl {
  final def act() {
    startUp()
    loopWhile(isRunning) {
      receive(neverMatchingHandler)
    }
  }
}

