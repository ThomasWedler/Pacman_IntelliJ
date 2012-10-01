package siris.core.svaractor

import handlersupport.Types.HandlerContinuation
import reflect.ClassManifest
import actors.Actor
import java.util.UUID

// Static methods for SVars.
/**
 * This is the companion object of the SVar trait.
 *
 * It has two methods to create a new State Variables.
 */
trait SVarObjectInterface {
  // Create a new SVar with initial value and owner.
  /**
   * This method creates a new State Variable at the given actor.
   * The State Variable will have the given value and will have
   * the given and not changeable datatype of the given value.
   * The method blocks until the state variable can be provided.
   *
   *
   * @param value The initial value of the State Variable
   * @param owner The owner of the state variable. Must not be null.
   * @tparam T The datatype of the state variable.
   * @return The created state variable. Never returns null.
   */
  def apply[T](value: T, owner: SVarActor): SVar[T]

  // Create a new SVar with initial value and self. Fails if self is not an SVarActor.
  // throws NotSVarActorException
  /**
   * This method creates a new state variable on the calling actor. This
   * method must be called within a SVarActor, otherwise a NotSVarActorException is
   * thrown. The method blocks until the creation of the State Variable is completly
   * finished.
   *
   *
   * @param value The initial value of the State Variable
   * @tparam T The data type of the state variable
   * @return The created state variable. Never returns null.
   */
  def apply[T](value: T) : SVar[T]
}

/**
 * This trait represents a state variable. State Variables
 * are every time created trough the mechanism of the companion object.
 *
 * @tparam The datatype of the represented value.
 */
trait SVar[T]{

  /**
   * The unique id trough which a SVar is identified
   */
  def id: UUID

  /**
   * Overrides equals to only compare _id
   */
  final override def equals(other: Any) : Boolean = other match {
    case that: SVar[_] => id.equals(that.id)
    case _ => false
  }

  /**
   * Overrides hashCode to use the _id's hashCode method
   */
  final override def hashCode =
    id.hashCode

  //! the typeinfo of the type of the contained value
  val containedValueManifest : ClassManifest[T]

  // Assign a new value.
  /**
   *  This method sets a new value to the given State Variable. If the
   * State Variable is owned by the calling Actor the value gets written
   * immediately. If the state variable is owned by another actor, the other
   * actor get an advice to write the value, but it depends on it's own logic
   * if the value gets written or not.
   *
   * The method always returns immediately and never blocks.
   *
   * @param value The new value for the State Variable
   */
  def set(value: T)

  /**
   *  This method reads the current value, applies it the provided update method
   * and finally uses the result to update the local value
   *
   * @param updateMethod the method used to calculate a new value from the current value
   */
  def update( updateMethod : T => T )

  // Retrieve the current value. blocking version, throws not SVarActorException
  /**
   * This method returns the current value of the state variable. It blocks
   * until the value can be provided. This method must be implemented by using
   * Continuations.
   *
   * @return The value of the State Variable
   */
  def get : T @HandlerContinuation

  // Retrieve the current value. The supplied handler is used only once.
  /**
   * This method reads the current value of the State Variable. It does not
   * block and returnd immediately. The given consumer function at the parameter
   * gets called when the value has been provided. The consumer is only valid
   * one time and gets deleted after the value has been provided.
   *
   * The given handler is processed in the current actor that is calling the
   * method.
   *
   * If the State Variable belongs to the current actor, the value can be read
   * immediately. In that case the consumer function is processed immediately
   * and the get method returns after the consumer function has been completed.
   *
   * @param A function that consumes the value of the State Variable.
   */
  def get(consume: T => Unit)

  // self observes future value changes. The supplied handle is reused.
  /**
   * Calling this method will observe the given state variable. Every time the
   * value of the State Variable gets changed the given handler messages gets
   * called. The handler message is running in the actor that called the
   * observe method.
   *
   * Only one handler can be registered at one time. If the method gets called
   * again within the the same actor the old handler gets replaced.
   *
   * An actor can observe the own state variable.
   *
   * A change of the value is only be notified if the value really change. E.g.
   * a State Variable contains the value 1 and a write operation with the value 1
   * is performed, no observers will be notified, because the value has not changed.
   *
   * @param handler The handler, that gets called when the value of the State Variable has changed.
   */
  def observe(handler: T => Unit)

  // self observes future value changes. The supplied handle is reused.
  /**
   * Calling this method will observe the given state variable. Every time the
   * value of the State Variable gets changed the given handler messages gets
   * called. The handler message is running in the actor that called the
   * observe method.
   *
   * Only one handler can be registered at one time. If the method gets called
   * again within the the same actor the old handler gets replaced.
   *
   * An actor can observe the own state variable.
   *
   * A change of the value is only be notified if the value really change. E.g.
   * a State Variable contains the value 1 and a write operation with the value 1
   * is performed, no observers will be notified, because the value has not changed.
   *
   * @param handler The handler, that gets called when the value of the State Variable has changed.
   * @param ignoredWriters Value changes by SVarActors contained in this set are ignored.
   */
  def observe(ignoredWriters: Set[Actor] = Set())(handler: T => Unit)

  /**
   *  This method returns the last known owner of the state variable.
   *
   */
  def owner() : SVarActor

  // Assign a new owner.
  /**
   * This method assigns a new owner to the state variable. It does not block
   * and returns immediately. An owner change can be rejected by the current
   * owner.
   *
   * @param owner The new owner of the State Variable. Must not be null.
   */
  def owner(owner: SVarActor)

  // Stop observing this SVar.
  /**
   * Calling this method stops observing the State Variable. The registred
   * handler gets removed and the current actor is not informed about changes
   * of the value any more.
   */
  def ignore()
}


