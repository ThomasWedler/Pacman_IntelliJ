package siris.core.svaractor.synclayer

/* author: dwiebusch
 * date: 03.09.2010
 */

//Sync-Layer
/**
 * This trait provides an actor that can provide a synced state of it's State
 * Variables.
 */
trait SVarSyncLayer{
  // caller registers for sync messages
  /**
   * To get only a consistent view of the State Variables owned by an actor O,
   * another actor must call the observe method of O. This is a different method
   * than the observe method of the State Variable. After calling this method
   * the observed actor sends sync notification messages to all observing actors
   * whenever all of the state variables owned by it are in a consistent state.
   * When receiving such a sync notification message, an observing actor knows
   * the following:
   * The set of O's State Variables, observed using the State Variable observe
   * mechanism, is transformed into a new consistent state by applying all
   * changes received until now.
   * This feature reduces the amount of data that is shared between actors when
   * using the synchronization mechanism compared to the previous
   * implementation, because an actor can also observe subsets of State
   * Variables owned by another actor. There is no overhead caused by sending
   * unneeded data.
   *
   */
  def observe()

  /**
   * This method switches the change notification back to normal immediate change
   * notification.
   */
  def ignore()

  /**
   * Calling this method propagates a consistent state to all actors in
   * observe mode.
   */
  protected def push()
}
