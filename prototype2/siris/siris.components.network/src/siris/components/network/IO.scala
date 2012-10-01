package siris.components.network

import siris.core.svaractor.{SVar, SVarActorHW}
import actors.Actor
import java.net._
import java.io._
import siris.core.svaractor.SVarActorHW

/**
 *  Actor handling the low level network functionality.
 * @param component         Component that created this actor.
 */
class IO( val component: Actor ) extends SVarActorHW {

  private val bufferSize = 1024
  private var socket : DatagramSocket = null
  private var port : Short = 0

  Actor.self ! Update( Actor.self )

  addHandler[Update] {
    case Update( sender ) => {
      // check for received messages
      if(socket != null){
        // wait for new packet
        var noNew : Boolean = false
        val buffer = new Array[Byte](bufferSize)
        val datagram = new DatagramPacket(buffer, bufferSize)
        try{
          socket.receive(datagram)
        } catch {
          case e : Exception => {
            noNew = true
          }
        }

        if(!noNew){
          // de-serialize
          val bais = new ByteArrayInputStream(datagram.getData())
          val ois = new ObjectInputStream(bais)
          val obj = ois.readObject()
          val packet = obj.asInstanceOf[Packet]
          component ! Received(packet.sender, packet.receiver, packet.commands)
        }
      }

      // make sure component stays updated
      Actor.self ! Update( Actor.self )
    }
  }

  addHandler[Send] {
    case Send ( sender, receiver, commands ) => {
      if(socket != null){
        // build a new packet
        val packet = new Packet
        packet.sender = sender
        packet.receiver = receiver
        packet.commands = commands

        // serialize packet
        val baos = new ByteArrayOutputStream()
        val oos = new ObjectOutputStream(baos)
        oos.writeObject(packet)
        val buffer = baos.toByteArray()

        // create and send datagram
        val datagram = new DatagramPacket(buffer, buffer.size)
        datagram.setSocketAddress(receiver.address)
        socket.send(datagram)
      }
    }
  }

  addHandler[SetPort] {
    case SetPort (newPort) => {
      // close socket
      if(socket != null){
        socket.close()
        socket = null
      }
      // create a new port
      try{
        socket = new DatagramSocket(newPort)
        port = newPort
        socket.setSoTimeout(50)
      } catch {
        case e : Exception => {
          error("failed to create socket")
          component ! Error
        }
      }
    }
  }
}