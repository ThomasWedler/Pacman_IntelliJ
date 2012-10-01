package siris.core.svaractor

import handlersupport.HandlerSupport
import actors.Actor
import siris.core.helper.Loggable

/* author: dwiebusch
 * date: 19.09.2010
 */

/**
 * This is the companion object of the SVarActor, an actor that can handle
 * State Variables.
 */
trait SVarActorObjectInterface {

  //returns the currently running actor
  /**
   * This method returns the current actor from which the method is called.
   * If the calling actor is not a SVarActor a NotSVarActorException is thrown
   */
  def self: SVarActor

  //creates a new SVarActor
  /**
   * This method creates a new SVarActor.
   */
  def actor(init: => Unit): SVarActor
}


/**
 * This trait is an actor that can handle State Variables and fullfills the
 * required notifications.
 */
trait SVarActor extends Actor with HandlerSupport with Loggable {

  //
  /**
   * A call of this method advices the actor to shutdown. It should remove
   * all own handlers and change the owner of all owned state variables.
   *
   * When an actor shuts down its State Variables are owner-changed to an
   * observer where possible. The remaining State Variables are transferred
   * to the next actor accessing them. The actor which is shutting down keeps
   * alive until all State Variable ownerships are transferred. However the
   * actor shutting down may remove references to its internal representation
   */
  def shutdown()
}