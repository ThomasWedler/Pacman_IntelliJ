package siris.core.svaractor

import actors.Actor

/* author: dwiebusch
 * date: 19.09.2010
 */

/**
 * This trait provides a base for the notification of observers of a State
 * Variable.
 */
trait SVarData{

  /**
   * This method adds an observer.
   *
   * @param a The actor that is to be added an an observer.
   */
  def addObserver( a : SVarActor, ignoredWriters: Set[Actor] )


  /**
   * This methdo removes an observers
   *
   * @param a The actor that is to be removed as an observer.
   */
  def removeObserver( a : SVarActor)

  //notify observers
  /**
   * Calling this methods leads into a notification of all observers about a
   * change of values.
   */
  def notifyWrite(writer: Actor)
}

