package siris.core.svaractor

import handlersupport.HandlerSupport
import actors.Actor

/**
 * User: stephan_rehfeld
 * Date: 22.09.11
 * Time: 20:37
 */

// ToDo document!

case class Ping( sender: Actor, identifier : Any, timestamp : Long = System.nanoTime()) extends SIRISMessage

case class Reply( sender : Actor, identifier : Any, originalTimestamp : Long, replyTimestamp : Long = System.nanoTime() )

trait PingableActor extends HandlerSupport {
  addHandler[Ping]( {
    case Ping( sender, identifier, timestamp ) =>
      sender ! Reply( Actor.self, identifier, timestamp )
  })

}

trait PingReplyOutputActor extends HandlerSupport {
  addHandler[Reply]( {
    case Reply( sender, identifier, originalTimestamp, replyTimeStamp ) =>
      val time = System.nanoTime()
      println( "Reply for \"" + identifier + "\". source->source=" + ((time - originalTimestamp)/1000000000.0) + "s, source->target=" + ((replyTimeStamp - originalTimestamp)/1000000000.0) + "s, target->source=" + ((time - replyTimeStamp)/1000000000.0) + "s." )
  })
}