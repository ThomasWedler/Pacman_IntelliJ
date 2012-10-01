package siris.core.component

import siris.core.entity.component.LowerEntityConfigLayer
import siris.core.svaractor.{SIRISMessage, SVarActor}
import siris.core.entity.description.{Semantics, SVal, SValList}
import scala.collection.mutable
import actors.Actor

/* author: dwiebusch
 * date: 27.08.2010
 */

/**
 *
 *  Used to create ConfigureComponentMessages
 */
object ConfigureComponent {
  /**
   *  Creates a ConfigureComponentMessage
   */
  def apply(sender: Actor, createParams: SValList) = ConfigureComponentMessage(sender, createParams)
  /**
   *  Creates a ConfigureComponentMessage using params to create a new SValList
   */
  def apply(sender: Actor, params: SVal[_]*) = ConfigureComponentMessage(sender, new SValList(params:_*))
  /**
   *  Creates a ConfigureComponentMessage using Actor.self as sender
   */
  def apply(createParams: SValList) = ConfigureComponentMessage(Actor.self, createParams)
  /**
   *  Creates a ConfigureComponentMessage using Actor.self as sender and params to create a new SValList
   */
  def apply(params: SVal[_]*) = ConfigureComponentMessage(Actor.self, new SValList(params:_*))
}

/**
 *
 *  This message is sent to a component to configure it initially
 *
 * @param sender The sender of this message
 * @param createParams A set of CVars that this component needs to be initialized
 *                     Obligatory and optional CVars for individual Components should be defined in their respective documentations.
 */
case class ConfigureComponentMessage(sender: Actor, createParams: SValList) extends SIRISMessage{
  def send(receiver : Actor){
    receiver ! this
  }
}

/**
 * @author Stephan Rehfeld
 *
 *  This message is sent by a component when the configuration process is finished.
 *
 * It will be sended to the sender of the configuration messages.
 *
 * @param sender The sender of the message. The component.
 */
case class ComponentConfigured( sender: Actor ) extends SIRISMessage


/**
 *  trait to provide SVarActors with EntityConfigLayer
 */
trait Component extends SVarActor with LowerEntityConfigLayer {
  /**
   *  (re)configure the component
   * @param params the configuration params
   */
  protected def configure(params : SValList)

  //add handler for configuration msgs
  addHandler[ConfigureComponentMessage]{ msg =>
    configure(msg.createParams)
    msg.sender ! ComponentConfigured(Actor.self)
  }

  // register this component
  Component(this)
}

/**
 *  object for process-wide component registration
 */
object Component{
  //! the registered components by name
  private val registeredComponents = mutable.Map[Symbol, Component]()
  //! the registered components by type
  private var componentsByType = Map[SVal[Semantics], List[Component]]()

  /**
   *  retrieve an registered component
   * @param name the name of the component to be retrieved
   * @return the requested component 
   */
  def apply( name : Symbol ) : Component = registeredComponents.get(name) match {
    case None => throw new ComponentDoesNotExistException(name)
    case Some(comp) => comp
  }

  /**
   *  retrieve registered components by type
   * @param componentType the type of the components to be retrieved
   * @return the requested components
   */
  def apply( componentType : SVal[Semantics] ) : List[Component] =  componentsByType.get(componentType) match {
    case None => throw new ComponentTypeDoesNotExistException(componentType.value.toSymbol)
    case Some(compList) => compList
  }

  /**
   *  registers a component
   * @param comp the component to register
   *
   */
  def apply(comp : Component) = {
    componentsByType = componentsByType.updated(comp.componentType, comp :: componentsByType.getOrElse(comp.componentType, Nil) )
    registeredComponents += comp.componentName -> comp
  }

  def shutdownAll() {
    registeredComponents.foreach(_._2.shutdown())
  }
}


//! Exception which is thrown if a requested component was not registered yet
class ComponentDoesNotExistException(val name : Symbol) extends Exception{
  override def toString = "ComponentDoesNotExistException: " + name.name
}
//! Exception which is thrown if a requested component was not registered yet
class ComponentTypeDoesNotExistException(val name : Symbol) extends Exception{
  override def toString = "ComponentTypeDoesNotExistException: " + name.name
}