package siris.core.helper

import java.util.concurrent.ScheduledExecutorService


/**
 * User: martin
 * Date: Dec 16, 2010
 */

object SchedulerAdjustment {

  // we want more than 4 actors, so we have to do some adjustments
  def adjustTheScheduler() {
    val sched = new scala.actors.scheduler.ResizableThreadPoolScheduler(false)
    scala.actors.Scheduler.impl.shutdown()
    scala.actors.Scheduler.impl = sched
    sched.start()
  }
}