package siris.components.worldinterface

import siris.core.entity.Entity
import actors.Actor
import concurrent.SyncVar
import siris.core.component.Component
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.components.eventhandling.{EventDescription, Event, EventProvider, EventHandler}
import siris.core.svaractor.{SVarActorImpl, SVar}
import java.util.UUID

/* author: dwiebusch
 * date: 10.09.2010
 */

/**
 * The World Interface. This object shall be used to provide to state value known to the entire programm, as well as
 * for actors and components. (In fact this is only a wrapper around the WorldInterfaceActor, which is doing all the
 * work)
 *
 * For now, one has to ensure that the id's of registered components/entities/state values/actors are unique, later
 * this shall be done automatically
 *
 *
 * @author dwiebusch
 * date: 02.07.2010
 */
object WorldInterface{
  private val worldInterfaceActor = (new WorldInterfaceActor).start().asInstanceOf[WorldInterfaceActor]
  def shutdown() {
    worldInterfaceActor.shutdown()
  }
  /**
   * registers a component
   *
   * @param component the component to be registered. its component name will be used for registration
   */
  def registerComponent(component : Component) {
    worldInterfaceActor ! ComponentRegisterRequest(component.componentName, component)
  }

  /**
   * registers an actor
   *
   * @param name the name under which the actor will be accessible after being registered
   * @param actor the actor to be registered
   */
  def registerActor(name: Symbol, actor: Actor) {
    worldInterfaceActor ! ActorRegisterRequest(name, actor)
  }

  /**
   * registers an entity
   *
   * @param name the name under which the entity will be accessible after being registered
   * @param e the entity to be registered
   */
  def registerEntity(name : Symbol, e : Entity) {
    worldInterfaceActor ! EntityRegisterRequest(name :: Nil, e)
  }

  def registerEntity(name : List[Symbol], e : Entity) {
    worldInterfaceActor ! EntityRegisterRequest(name, e)
  }

  def unregisterEntity(e : Entity) {
    worldInterfaceActor ! EntityUnregisterRequest(e)
  }


  /**
   * registers a given EventHandler for all events having the same name as the given event
   *
   * @param handler the handler to be registered
   * @param event an event the handler is registered for
   */
  def requireEvent( handler : EventHandler, event : EventDescription ) {
    worldInterfaceActor ! RegisterHandlerMessage(handler, event)
  }

  /**
   * registers an EventProvider and stores the event it will provide. Furthermore tells all
   * EventHandlers that have requiered the event (more precisely: an event with the same name)
   * of the existence of the Provider
   *
   * @param provider the EventProvider to be registered
   * @param event the event that the provider provides
   */
  def provideEvent(provider : EventProvider,  event : EventDescription ) {
    worldInterfaceActor ! ProvideEventMessage(provider, event)
  }

  /**
   * removes an EventHandler for one specific or all events, after what it won't be notified about
   * new Provider providing the event / all events the handler was registered for
   *
   * @param handler the EventHandler to be unregistered
   * @param event the event the handler will be unregistered from (optional)
   */
  def removeEventHandler( handler : EventHandler, event : Option[EventDescription] = None ) {
    worldInterfaceActor ! UnRegisterHandlerMessage( handler, event )
  }

  /**
   * removes an EventProvider from the list of registered providers, therefore new handlers won't
   * be notified about this specific provider either for the given event or all events (if no event is given)
   *
   * @param provider the provider to be unregistered
   * @param event the event which will no more be provided (optional) 
   */
  def removeEventProvider( provider : EventProvider, event : Option[Event] = None ) {
    worldInterfaceActor ! UnRegisterProviderMessage( provider )
  }

  /**
   *  creates a new state value
   *
   * @param desc information on the svar to be created
   * @param entity name of the entity which shall contain the new state value
   */
  def createStateValue[T](desc : ConvertibleTrait[T], value : T, entityName : Symbol) {
    worldInterfaceActor ! StateValueCreateRequest(desc, value, entityName :: Nil)
  }

  def createStateValue[T](desc : ConvertibleTrait[T], value : T, entityName : List[Symbol]) {
    worldInterfaceActor ! StateValueCreateRequest(desc, value, entityName)
  }

  /**
   * sets a known state value
   *
   * @param c information on the svar to be changed
   * @param container the containing entity
   * @param value the new value
   */
  def setStateValue[T](c : ConvertibleTrait[T], container : Symbol, value : T) {
    worldInterfaceActor.handleMessage( StateValueSetRequest(c, container :: Nil, value) )
  }

  def setStateValue[T](c : ConvertibleTrait[T], container : List[Symbol], value : T) {
    worldInterfaceActor.handleMessage( StateValueSetRequest(c, container, value) )
  }

  /**
   * adds a new WorldInterfaceEvent to be triggered, when the given (external) state value changes
   *
   * @param stateValue the state value to be observed
   * @param container the name of the entity containing the svar
   * @param trigger the WorldInterfaceEvents name being triggered on the state value change
   */
  def addValueChangeTrigger[T](stateValue : SVar[T], container : Symbol, trigger : Symbol) {
    worldInterfaceActor ! ExternalStateValueObserveRequest(stateValue, container :: Nil, trigger)
  }

  def addValueChangeTrigger[T](stateValue : SVar[T], container : List[Symbol], trigger : Symbol) {
    worldInterfaceActor ! ExternalStateValueObserveRequest(stateValue, container, trigger)
  }

  /**
   * adds a new WorldInterfaceEvent to be triggered, when the given (internal) state value changes
   *
   * @param c information on the state value to be observed (has to be known by the WorldInterfaceActor)
   * @param entityName the name of the entity containing the specified state value
   * @param trigger the WorldInterfaceEvents name being triggered on the state value change
   */
  def addValueChangeTrigger[T]( c : ConvertibleTrait[T], entityName : Symbol, trigger : Symbol) {
    worldInterfaceActor ! InternalStateValueObserveRequest(c, entityName :: Nil, trigger)
  }

  def addValueChangeTrigger[T]( c : ConvertibleTrait[T], entityName : List[Symbol], trigger : Symbol) {
    worldInterfaceActor ! InternalStateValueObserveRequest(c, entityName, trigger)
  }

  /**
   * forwards a message to an actor known to the WorldInterface (when a lot more than one message has to be sent,
   * looking up the actor and sending the messages directly should be a lot faster)
   *
   * WARNING: ordering of messages is not guaranteed when using forwardMessage and sending messages directly. So DON'T
   * mix these channels when two messages have to arrive in the same order they were sent
   *
   * @param receiver the id under which the receiving actor was registered
   * @param msg the message to be forwarded
   */
  def forwardMessage( receiver : Symbol, msg : Any) {
    worldInterfaceActor.handleMessage( ForwardMessageRequest(receiver, msg) )
  }

  /**
   * creates a new entity
   *
   * @param name the id of the created entity
   */
  def createEntity(name: Symbol) {
    worldInterfaceActor.handleMessage( EntityCreateRequest(name :: Nil) )
  }

  def createEntity(name: List[Symbol]) {
    worldInterfaceActor.handleMessage( EntityCreateRequest(name) )
  }

  /**
   * retreives all known actors' names
   *
   * @return a list containing the names of all known actors
   */
  def getActorList =
    blockingHandler[Option[List[Symbol]]]( ActorEnumerateRequest(_)).getOrElse(Nil)

  def handleActorList(handler :  Option[List[Symbol]] => Unit) {
    nonBlockingHandler[Option[List[Symbol]]]( ActorEnumerateRequest(_), handler )
  }

  /**
   * retrieves a specific actor
   *
   * @param name the actors name
   * @return the requested actor or None
   */
  def lookupActor(name : Symbol) : Option[Actor] =
    blockingHandler(ActorLookupRequest(name, _))

  def handleActor( name : Symbol )( handler : Option[Actor] => Unit ) {
    nonBlockingHandler[Option[Actor]](ActorLookupRequest(name, _), handler)
  }

  /**
   * retrieves a specific state value
   *
   * @param c information on the requested state value
   * @param container the name of the entity containing the specific state value
   * @return the requested state value or None
   */
  def lookupStateValue[T]( c : ConvertibleTrait[T], container : Symbol ) : Option[SVar[T]] =
    blockingHandler(StateValueLookupRequest(c, container :: Nil, _))

  def handleStateValue[T]( c : ConvertibleTrait[T], container : Symbol )( handler : Option[SVar[T]] => Unit ) {
    nonBlockingHandler[Option[SVar[T]]](StateValueLookupRequest(c, List(container), _), handler)
  }

  def lookupStateValue[T]( c : ConvertibleTrait[T], container : List[Symbol] ) : Option[SVar[T]] =
    blockingHandler(StateValueLookupRequest(c, container, _))

  def handleStateValue[T]( c : ConvertibleTrait[T], container : List[Symbol] )( handler : Option[SVar[T]] => Unit ) {
    nonBlockingHandler[Option[SVar[T]]](StateValueLookupRequest(c, container, _), handler)
  }

  /**
   * retrieves a specific entity
   *
   * @param name the entity's name
   * @return the requested entity or None
   */
  def lookupEntity(name : Symbol) : Option[Entity] =
    blockingHandler(EntityLookupRequest(name :: Nil, _))

  def handleEntity( name : Symbol )( handler : Option[Entity] => Any ) {
    nonBlockingHandler[Option[Entity]]( EntityLookupRequest( name :: Nil, _), handler )
  }

  /**
   * retrieves a specific component
   *
   * @param name the component's name
   * @return the requested component or None
   */
  def lookupComponent(name : Symbol) : Option[Component] =
    blockingHandler(ComponentLookupRequest(name, _))

  def handleComponent(name : Symbol)( handler : Option[Component] => Any ) {
    nonBlockingHandler[Option[Component]]( ComponentLookupRequest(name, _ ), handler )
  }


  /**
   * retreive an registered svars value
   *
   * @param c info on the svar
   * @param entity the name of the entity, containing the svar
   * @return the value of the svar or None if an error occured (e.g. svar was not found)
   */
  def readSVar[T]( c : ConvertibleTrait[T], entity : Symbol ) : Option[T] =
    blockingHandler(SVarReadRequest(c, entity :: Nil, _))

  def handleSVarValue[T]( c : ConvertibleTrait[T], entity : Symbol )( handler : Option[T] => Unit ) {
    nonBlockingHandler[Option[T]](SVarReadRequest(c, entity :: Nil, _), handler)
  }

  def readSVar[T]( c : ConvertibleTrait[T], entity : List[Symbol] ) : Option[T] =
    blockingHandler(SVarReadRequest(c, entity, _))

  def handleSVarValue[T]( c : ConvertibleTrait[T], entity : List[Symbol] )( handler : Option[T] => Unit ) {
    nonBlockingHandler[Option[T]](SVarReadRequest(c, entity, _), handler)
  }


  /**
   * helper function, used to retreive values from svars, when not inside of an svaractor
   *
   * @param svar the value
   * @return the value of the svar 
   */
  def readSVarBlocking[T]( svar : SVar[T] ) : T =
    blockingHandler(ReadRequest(svar, _))

  def readSVar[T]( svar : SVar[T])( handler : T => Unit ) {
    nonBlockingHandler[T](ReadRequest(svar, _), handler)
  }

  def getRegisteredEntities( path : List[Symbol]) : Option[Set[Entity]]=
    blockingHandler(EntityGroupLookupRequest(path, _))

  def handleRegisteredEntities( path : List[Symbol] )( handler : Option[Set[Entity]] => Any ) {
    nonBlockingHandler[Option[Set[Entity]]](EntityGroupLookupRequest(path, _), handler)
  }

  def getRegisteredComponents : Map[Symbol, Component] =
    blockingHandler(AllComponentsLookupRequest(_))

  def handleRegisteredComponents(handler : Map[Symbol, Component] => Unit) {
    nonBlockingHandler[Map[Symbol, Component]](AllComponentsLookupRequest(_), handler)
  }

  def registerForCreationOf( path : List[Symbol] ) {
    worldInterfaceActor ! ListenForRegistrationsMessage( Actor.self, path )
  }

  /**
   *
   * this one is a bit advanced, so here is what happens:
   * the function takes two parameters, one function that instanciates the message to be send and the handler which
   * shall be executed when an answer to that message is sent.
   * Initially an ID is generated and stored to identify the message which is sent. Then a single-use-handler is
   * installed, which uses that generated id. The installed handler simply calls the function "handler" which is
   * provided as a parameter, applying the value returned by the worldInterfaceActor.
   * Finally a message is sent to the worldInterfaceActor, which contains an handler (executed by the
   * worldInterfaceActor), that sends the id, value tuple back, causing the invocation of the installed handler
   *
   */
  protected def nonBlockingHandler[T](msg : (T => Unit) => Any, handler : T => Any) {
    type Tup = Tuple2[UUID, T]
    val (id, self) = (UUID.randomUUID, SVarActorImpl.self)
    self.addSingleUseHandler[Tup]({ case (n, c) if (n.equals(id)) => handler(c) } : PartialFunction[Tup, Unit])
    worldInterfaceActor.handleMessage( msg( x => self ! (id, x) ) )
  }

  protected def nonBlockingHandlerTest( msg : HandlerMSG[_] ) {
    msg.registerHandlerAndSend( SVarActorImpl.self, worldInterfaceActor )
  }

  protected def blockingHandler[T](msg : (T => Unit) => Any ) : T = {
    val future = new SyncVar[T]
    worldInterfaceActor.handleMessage( msg( future.set ))
    future.take()
  }
}