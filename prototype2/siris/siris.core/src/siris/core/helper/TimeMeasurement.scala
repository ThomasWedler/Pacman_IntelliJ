package siris.core.helper

import java.util.concurrent.{TimeUnit, Executors}
import compat.Platform
import actors.{TIMEOUT, Actor}
import siris.core.svaractor.handlersupport.HandlerSupport
import java.util.UUID

/**
 * User: martin
 * Date: 07.06.11
 * Time: 14:51
 */

trait TimeMeasurement extends HandlerSupport {

  private var oldTime: Long = System.nanoTime()
  private var startStopTime: Long = 0

  /**
   * The period of one frame in nanoseconds. This is in timeToNextFrame.
   */
  protected var framePeriod: Long = 16L * 1000L * 1000L

  /**
   * The message used by requestWakeUpCall. Can be overwritten if needed.
   */
  protected def WakeUpMessage: Any = WakeUp()

  /**
   * The default wake up message
   */
  case class WakeUp()

  private case class Execute(id: UUID = UUID.randomUUID())

  /**
   *  Provides usefull functions for Period
   */
  object Period {
    def fromNanos(n: Long): Period =
      new Period(n / 1000000, (n % 1000000).toInt )

    def apply(milliseconds: Long, nanoseconds: Int = 0) =
      new Period(milliseconds, nanoseconds)

    val Zero = new Period(0, 0)
  }

  /**
   *  Stores time periods.
   */
  class Period(val milliseconds: Long, val nanoseconds: Int = 0) {
    def toSeconds : Float =
      (milliseconds / 1000.0f) + (nanoseconds / 1000000000.0f)

    def toMillis : Float =
      toNanos / 1000000f

    def toNanos : Long =
      (milliseconds * 1000000L) + nanoseconds

    def >(that: Period): Boolean = {
      if(milliseconds > that.milliseconds) true
      else if(milliseconds < that.milliseconds) false
      else nanoseconds > that.nanoseconds
    }

    override def toString =
      "Period(" + milliseconds + "ms, " + nanoseconds + "ns)"
  }

//  /**
//   *  An actor that send a JBSimulateFrame after a given time to a given target.
//   */
//  private class WakeUpActor(t: Period, a: Actor) extends Actor {
//    def act() {
//      Thread.sleep(t.milliseconds, t.nanoseconds)
//      a ! WakeUpMessage
//    }
//  }

  /**
   *  An actor that send a Message at a given time to a given target.
   */
  private class WakeUpActor(atNanoTime: Long, a: Actor, msg: Any = WakeUpMessage) extends Actor {
    def act() {
      val nowInMillis = System.nanoTime() / 1000000L
      val atInMillis = atNanoTime / 1000000L
      if(nowInMillis >= atInMillis) a ! msg
      else reactWithin(atInMillis-nowInMillis){
        case TIMEOUT => a ! msg
      }
    }
  }

  /**
   *  Returns the time passed between the last invocation of getDeltaT and now
   */
  protected def getDeltaT: Period = {
    val newTime = System.nanoTime()
    val deltaT = (newTime - oldTime)
    oldTime  = newTime
    Period.fromNanos(deltaT)
  }

  /**
   *  Starts a time measurement
   */
  protected def startTimeMeasurement() {
    startStopTime = System.nanoTime()
  }

  /**
   *  Returns the time passed between the last invocation of startTimeMeasurement and now
   */
  protected def stopTimeMeasurement(): Period =
    Period.fromNanos(System.nanoTime() - startStopTime)

  /**
   *    Returns the time left until the next frame has to be processed.
   * Assumes a frame every frameFrequency nanoseconds and the start of the last frame
   *          at the time of the last startTimeMeasurement invocation.
   */
  protected def timeToNextFrame(): Period = {
    val diff = System.nanoTime() - startStopTime
    val timeToNextFrame = framePeriod - diff
    if(timeToNextFrame > 0) Period.fromNanos(timeToNextFrame)
    else Period.Zero
  }

  /**
   *  Start an actor that sends the calling actor a WakeUpMessage after the given period of time.
   */
  protected def requestWakeUpCall(in: Period) {
    if(in != Period.Zero) new WakeUpActor(System.nanoTime() + in.toNanos, Actor.self).start()
    else Actor.self ! WakeUpMessage
  }

  /**
   * Uses the HandlerSupportTrait's addSingleUseHandlerPF and the WakeUpActor to schedule the execution of f.
   */
  protected def scheduleExecution(delay: Period)(f: => Unit) {
    val execMsg = Execute()
    addSingleUseHandlerPF[Execute]{case msg if(msg.id == execMsg.id) => f}
    if(delay != Period.Zero) new WakeUpActor(System.nanoTime() + delay.toNanos, Actor.self, execMsg).start()
    else Actor.self ! execMsg
  }

  protected def scheduleExecution(delayInMillis: Long)(f: => Unit) {
    scheduleExecution(Period(delayInMillis))(f)
  }
}
