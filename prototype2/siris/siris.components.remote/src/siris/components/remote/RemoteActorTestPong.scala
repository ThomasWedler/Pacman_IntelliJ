/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/28/11
 * Time: 2:22 PM
 */
package siris.components.remote;

import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._

object RemoteActorTestPong {
    def main(args: Array[String]) : Unit = {
        val port = 9000 // args(0).toInt
        val pong = new RemotePong(port)
        pong.start()
    }
}

class RemotePong(port: Int) extends Actor {
  def act() {
    RemoteActor.classLoader = getClass().getClassLoader()
    alive(port)
    register('Pong, self)

    while (true) {
      receive {
        case Ping =>
          Console.println("Pong: ping")
          sender ! Pong
        case Quit =>
          Console.println("Pong: stop")
          exit()    // (9)
      }
    }
  }
}