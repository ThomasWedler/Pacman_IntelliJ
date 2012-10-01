package siris.core

import helper.{TimeMeasurement, SchedulerAdjustment}
import svaractor.SVarActorLW

/**
 * User: dwiebusch
 * Date: 26.05.11
 * Time: 09:47
 */

abstract class SIRISApplication{
  private val appActor = new SVarActorLW with TimeMeasurement {
    override def startUp() {
      SchedulerAdjustment.adjustTheScheduler()
      createComponents()
      createEntities()
      finishConfiguration()
    }

    addHandler[ScheduleShutdownIn]{ case msg =>
      scheduleExecution(msg.millis) {
        shutdown()
        // TODO: Exit should not be necessary. Check why actors do not exit properly.
        System.exit(0)
      }
    }
  }

  private case class ScheduleShutdownIn(millis: Long)

  protected def start() {
    SchedulerAdjustment.adjustTheScheduler()
    initialize()
    appActor.start()
  }

  protected def shutdown() {
    appActor ! ScheduleShutdownIn(500L)
  }

  protected def initialize() {
  }

  protected def createComponents()
  protected def createEntities()
  protected def finishConfiguration()
}