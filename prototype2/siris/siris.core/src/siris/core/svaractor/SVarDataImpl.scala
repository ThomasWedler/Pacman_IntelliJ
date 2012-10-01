package siris.core.svaractor

import actors.Actor
import scala.ref.WeakReference

/**
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 * @date: 03.08.2010
 */


/*
 * @todo DOCUMENT THIS FILE!
 */
trait SVarDataBase extends SVarData  {
  protected var observers = Map[SVarActor, Set[Actor]]()
  val svar : WeakReference[SVar[_]]

  def write[V]( writer: Actor, value : V )
  def read[V] : V

  def removeObserver(a: SVarActor) {
    observers = observers - a
  }

  def addObserver(a: SVarActor, ignoredWriters: Set[Actor]) {
    observers = observers + (a -> ignoredWriters)
  }

  def isObservedBy( a : SVarActor ) : Boolean =
    observers.contains(a)

  def getObservers =
    observers
}

class SVarDataImpl[T](var data : T, val svar : WeakReference[SVar[T]]) extends SVarDataBase{
  def notifyWrite(writer: Actor) {
    svar.get match {
      case Some(ref) => observers.foreach{ kvPair =>
        if(!kvPair._2.contains(writer)) kvPair._1 ! NotifyWriteSVarMessage[T]( Actor.self, ref, data) }
      case None =>
    }
  }

  def write[V]( writer: Actor, value : V ) {
    data = value.asInstanceOf[T]
    notifyWrite(writer)
  }

  def read[V] : V = data.asInstanceOf[V]
}
