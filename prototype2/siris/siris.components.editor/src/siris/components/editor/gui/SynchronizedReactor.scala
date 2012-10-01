package siris.components.editor.gui

import swing.Reactor
import javax.swing.SwingUtilities

/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 8/24/11
 * Time: 4:50 PM
 */

/**
 * Provides a method to add reactions that are executed in the java swing thread
 */
trait SynchronizedReactor extends Reactor {

  /**
   * Adds a reaction that is executed in the java swing thread by using
   * SwingUtilities.invokeLater
   */
  def addSynchronizedReaction(r: scala.swing.Reactions.Reaction) {
    reactions += {
      case e: scala.swing.event.Event if (r.isDefinedAt(e)) =>
        SwingUtilities.invokeLater(new Runnable {
          def run() {
            r.apply(e)
          }
        })
    }
  }
}