/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/28/11
 * Time: 2:19 PM
 */
package siris.components.remote

;

import scala.actors.Actor
import scala.actors.Actor._
import scala.actors.Exit
import scala.actors.remote.RemoteActor
import scala.actors.remote.RemoteActor._
import scala.actors.remote.Node

case object Ping
case object Pong
case object Quit

object RemoteActorTestPing {
  def main(args: Array[String]): Unit = {
    val port = 9001 // args(0).toInt
    val peer = Node("132.180.199.1", 9000) // Node(args(1), args(2).toInt)
    val ping = new RemotePing(port, peer, 16)
    ping.start()
  }
}

class RemotePing(port: Int, peer: Node, count: Int) extends Actor {
  trapExit = true

  // (1)

  def act() {
    RemoteActor.classLoader = getClass().getClassLoader()
    alive(port) // (2)
    register('Ping, self) // (3)

    val pong = select(peer, 'Pong) // (4)
    link(pong) // (5)

    var pingsLeft = count - 1
    pong ! Ping // (6)
    while (true) {
      receive {
        case Pong =>
          Console.println("Ping: pong")
          if (pingsLeft > 0) {
            pong ! Ping
            pingsLeft -= 1
          } else {
            Console.println("Ping: start termination")
            pong ! Quit // (7)
            // Terminate ping after Pong exited (by linking)
          }
        case Exit(pong, 'normal) => // (8)
          Console.println("Ping: stop")
          exit()
      }
    }
  }
}