package siris.core.svaractor

import handlersupport.HandlerSupportImpl
import ref.WeakReference
import actors.Actor
import scala.collection.mutable
import synclayer.PluggableSyncLayer
import scala.Some

/* author: Dennis Wiebusch
 * author: Stephan Rehfeld
 * date: 03.08.2010
 */

object SVarActorImpl extends SVarActorObjectInterface {
  def self: SVarActorImpl = Actor.self match {
    case actor : SVarActorImpl => actor
    case _ => throw NotSVarActorException
  }

  def actor(initF: => Unit) = new SVarActorImpl with PluggableSyncLayer{
    final def act() {
      initF
      loopWhile(isRunning) {
        receive(neverMatchingHandler)
      }
    }
  }
}

abstract class SVarActorImpl extends SVarActor with HandlerSupportImpl with PingableActor {
  //
  //
  // inner classes
  //
  //
  protected class ChangeOwnerStatus(val svar : SVar[_], val newOwner : SVarActor) {
    private def clearQueueFor( actor : Actor ) {
      heldMessages.get( actor ) match {
        case None =>
        case Some(queue) => {
          heldMessages -= actor
          // forward messages if the current actor was the sender, return to sender otherwise
          if (actor == Actor.self)
            queue.foreach( newOwner ! _ )
          else
            actor ! HeldMessagesMessage(Actor.self, svar, newOwner, queue.toList)
        }
      }
    }

    def apply( actor : Actor) : Option[mutable.Queue[SVarMessage]] =
      heldMessages.get(actor)

    def acknowledgeBy( actor : Actor ) =
      if (isAccepted) clearQueueFor( actor ) else heldAcknowledges += actor

    def pushMessage( msg : SVarMessage  ) {
      val queue = heldMessages.getOrElse(msg.sender, mutable.Queue[SVarMessage]())
      queue += msg
      heldMessages.update(msg.sender, queue)
    }

    def acceptChange() = {
      changeAccepted = true
      heldAcknowledges.foreach( clearQueueFor( _ ) )
      heldAcknowledges.clear()
      acknowledgeBy(Actor.self)
    }

    def isAccepted = changeAccepted

    private val heldMessages     = mutable.Map[Actor, mutable.Queue[SVarMessage]]()
    private val heldAcknowledges = mutable.Set[Actor]()
    private var changeAccepted   = false
  }

  //
  //
  // variable initialization
  //
  //


  // TODO: Currently not a WeakHashMap, because contains does not work on it. Needs to be fixed.
  /* Question: Did the get method work? In most cases we want to obtain the value, so why
   do we need contains? The following should work, too:
   data.get(key) match {
      case Some(value) => process(value)
      case None => do_whatever_has_to_be_done_if_value_is_not_contained()
   }
   if we don't need the value, we could just ignore it
  */
  private val data = mutable.HashMap[SVar[_],SVarDataBase]()

  //! the map which holds queues for each svar that is beeing transferred
  private val heldMessages = mutable.Map[SVar[_], ChangeOwnerStatus]()

  //! the locally known owners
  private val sVarOwners = mutable.Map[SVar[_], SVarActor]()

  //!
  protected var isRunning : Boolean = true

  protected val sVarObserveHandlers = mutable.Map[SVar[_], Any => Unit]()

  //
  //
  // package private methods (at least most of them)
  //
  //

  //! !!! DO NOT USE THIS FUNCTION BUT THE SVARS OWNER() FUNCTION UNLESS YOU KNOW EXACTLY WHAT YOU ARE DOING !!!
  private[svaractor] def getLocallyKnownSVarOwner(svar: SVar[_]): Option[SVarActor] =
    sVarOwners.get(svar)

  private[svaractor] def getOrUpdateSVarOwner(svar: SVar[_], newOwner : SVarActor) : SVarActor =
    sVarOwners.getOrElseUpdate(svar, newOwner)

  private[svaractor] def addSVarObserveHandler(svar : SVar[_], handler : Any => Unit ) {
    sVarObserveHandlers.update(svar, handler)
  }

  private[svaractor] def removeSVarObserveHandlers(svar : SVar[_]) =
    sVarObserveHandlers -= svar

  private[svaractor] def addSVarOwner(svar: SVar[_], owner : SVarActor) {
    sVarOwners += svar -> owner
  }

  private[svaractor] def changeOwner[T](sender    : Actor,
                                        svar      : SVar[T],
                                        newOwner  : SVarActor,
                                        value     : Option[T] = None) : Boolean = {
    if (this == newOwner){
      if (!this.isOwnerOf(svar)) value match {
        case Some(_data) => insertSVar(svar, _data)
        case None => throw new Exception
      }
      sender ! AcceptSVarMessage(this, svar)
    }
    else if (isOwnerOf(svar)) heldMessages.get(svar) match {
      case Some(changeOwnerStatus) if (changeOwnerStatus.isAccepted) => svar.owner( newOwner ) //!!! is this correct? mustn't we tell "ownerHasChanged"?
      case Some(changeOwnerStatus) => changeOwnerStatus.pushMessage(ChangeOwnerOfSVarMessage(this, svar, newOwner))
      case None =>
        heldMessages(svar) = new ChangeOwnerStatus(svar, newOwner)
        newOwner ! BunchOfSirisMessagesMessage(
          this, OfferSVarMessage(this, getOriginal(svar), read(svar)) ::  moveObservers(svar, newOwner)
        )
    }
    //if nothing matched, the svar was unknown, so we return false
    else
      return false
    //if we got here, everything is ok, so we return true
    true
  }

  protected def moveObservers[T](svar : SVar[T], newOwner : SVarActor) : List[SIRISMessage] =
    data.get( svar ) match {
      case Some(_data) =>
        (for ( (observer, ignoredWriters) <- _data.getObservers) yield
          ObserveSVarMessage(observer, svar, ignoredWriters)).toList
      case None => Nil
    }

  protected def getSVarsObservedBy( observer : SVarActor) =
    data.filter( pair => pair._2.isObservedBy( observer) )

  protected def getObserversOf( svar : SVar[_] ) = data.get(svar) match {
    case Some(svardata) => svardata.getObservers
    case None => Map[SVarActor, Set[Actor]]()
  }

  /**
   * Creates a new SVar.
   * The data of this SVar is stored.
   * The weak reference that is stored with the data is used detrmine, if
   * the data is not needed any more.
   */
  private[svaractor] def createSVar[T]( value: T )( manifest : ClassManifest[T] ) : SVar[T] =
    insertSVar(new SVarImpl(this, manifest), value)

  private[svaractor] def createSVar[T]( tuple : Tuple2[T, ClassManifest[T]] ) : SVar[T] =
    createSVar(tuple._1)(tuple._2)

  //FIXME: use weak hashmap
  private def insertSVar[T]( sVar: SVar[T], value: T ) : SVar[T] = {
    data       += sVar -> new SVarDataImpl( value, new WeakReference( sVar ) )
    sVarOwners += sVar -> this
    sVar
  }

  //  private def removeSVar( svar : SVar[_] ) = {
  //    removeSVarOwner(svar)
  //    removeSVarData(svar)
  //  }

  private def removeSVarData( sVar: SVar[_] ) =
    data -= sVar

  private[svaractor] final def write[T]( writer: Actor, sVar: SVar[T], value: T ) {
    heldMessages.get(sVar) match {
      case Some(changeOwnerStatus) => changeOwnerStatus.pushMessage( WriteSVarMessage(this, writer, sVar, value) )
      case None => if( !read( sVar ).equals( value ) )
        internalWrite( writer, sVar, value )
    }
  }

  protected def internalWrite[T]( writer: Actor, sVar: SVar[T], value: T ) {
    data( sVar ).write( writer, value )
  }

  private[svaractor] def update[T]( writer : Actor, sVar : SVar[T], updateMethod : T => T ) {
    write(writer, sVar, updateMethod(read(sVar)))
  }

  private[svaractor] def read[T]( sVar: SVar[T] ) : T =
    data( sVar ).read

  private[svaractor] final def addObserver( sVar: SVar[_], a: SVarActor, ignoredWriters: Set[Actor] ) {
    heldMessages.get(sVar) match {
      case Some(changeOwnerStatus) => changeOwnerStatus.pushMessage( ObserveSVarMessage(a, sVar, ignoredWriters))
      case None => internalAddObserver( sVar, a, ignoredWriters )
    }
  }

  protected def internalAddObserver( sVar: SVar[_], a: SVarActor, ignoredWriters: Set[Actor] ) {
    data( sVar ).addObserver( a, ignoredWriters )
  }

  //
  //
  // private methods
  //
  //

  protected final def removeObserver( sVar: SVar[_], a: SVarActor ) {
    heldMessages.get(sVar) match {
      case Some(changeOwnerStatus) => changeOwnerStatus.pushMessage( IgnoreSVarMessage(a, sVar) )
      case None => internalRemoveObserver(sVar, a)
    }
  }

  protected def internalRemoveObserver(sVar: SVar[_], a: SVarActor ) {
    data( sVar ).removeObserver( a )
  }

  private def getOriginal[T](svar : SVar[T]) : SVar[T] = data.get(svar) match {
    case Some(_data) => _data.svar.get match {
      case Some(refToOrig) => refToOrig.asInstanceOf[SVar[T]]
      case _ => throw InvalidWeakRefException
    }
    case None => throw NotSVarOwnerException
  }

  private def isOwnerChangeInProgress( svar : SVar[_] ) : Boolean =
    heldMessages.get(svar).isDefined

  private def isOwnerOf(svar : SVar[_]) : Boolean =
    data.get(svar).isDefined

  private def updateSVarOwner(svar : SVar[_], newOwner : SVarActor) {
    sVarOwners.update(svar, newOwner)
  }

  private def handleOwnerDependentMsg[T <: SVarHoldingMessage]( handler : T => Unit )( msg : T ) {
    if ( isOwnerChangeInProgress(msg.sVar) )
      changeOwnerInProgessHandler( msg )
    else if ( isOwnerOf(msg.sVar) )
      handler( msg )
    else
      msg.sender ! SVarOwnerChangedMessage( this, msg.sVar, msg.sVar.owner(), msg )
  }

  private def createValueOfSVarMsg[T](sVar : SVar[T]) : ValueOfSVarMessage[T] =
    ValueOfSVarMessage(this, sVar, read(sVar))


  //  private def removeSVarOwner(svar : SVar[_]) {
  //    sVarOwners -= svar
  //  }

  //
  //
  //  public methods
  //
  //

  def startUp() {}

  def shutdown() {
    if (this != Actor.self)
      this ! Shutdown(Actor.self)
    else {
      isRunning = false
      //TODO: implement this
    }
  }

  //TODO: implement this
  override final def !( msg : Any ) {
    if (getState != Actor.State.Terminated ) super.!( msg ) //else applyHandlers(msg)
  }

  // Override such that obervable value messages are filtered
  // and processed transparently.
  //  private val debugOutputHandler : PartialFunction[Any, Any] = {
  //    case msg => {
  //      println("processing message : " +  msg)
  //      msg
  //    }
  //  }

  override def react (f: PartialFunction[Any, Unit]): Nothing = {
    super.react(  f orElse handlersAsPF orElse {case _ =>} )
  }

  // Override such that obervable value messages are filtered
  // and processed transparently.
  override def reactWithin(msec: Long)(f: PartialFunction[Any, Unit]): Nothing = {
    super.reactWithin(msec)( f.orElse(handlersAsPF.orElse{case _ =>}) )
  }

  override def receive[R](f : PartialFunction[Any, R]) : R = {
    super.receive( f orElse handlersAsPF orElse {case _ =>} )
    null.asInstanceOf[R]
  }

  override def receiveWithin[R](msec : Long)(f : PartialFunction[Any, R]) : R = {
    super.receiveWithin(msec)( f.orElse(handlersAsPF.orElse{case _ =>}) )
    null.asInstanceOf[R]
  }

  //------------------------------------//
  //                                    //
  //    handler definition section      //
  //                                    //
  //------------------------------------//

  private def changeOwnerInProgessHandler( msg : SVarHoldingMessage) {
    heldMessages.get(msg.sVar) match {
      case None => throw OwnerChangeNotInProgressException
      case Some(changeOwnerStatus) =>
        if (changeOwnerStatus(msg.sender).isEmpty)
          msg.sender ! SVarOwnerChangeInProgressMessage(SVarActorImpl.self, msg.sVar, changeOwnerStatus.newOwner)
        changeOwnerStatus.pushMessage(msg)
        heldMessages.update(msg.sVar, changeOwnerStatus)
    }
  }

  /* TODO: Currently using this, because there was an error with Actor.self. Redo it!
    using a lot of calls to the addHandler function will result in a lot of function
    calls when applying the handlers. If two or more handlers will never be modified
    individually, the should be added within one addHandler call.
     FIX this later, when we known which handlers have to be modified
  */
  addHandler[OfferSVarMessage[Any]]{
    msg => changeOwner( msg.sender, msg.sVar, this, Some( msg.value ) )
  }

  addHandler[Shutdown]{
    msg => shutdown()
  }


  addHandler[SVarOwnerChangeInProgressMessage]{ msg =>
    if(heldMessages.get(msg.sVar).isEmpty)
      heldMessages(msg.sVar) = new ChangeOwnerStatus(msg.sVar, msg.newOwner)
    msg.sender ! AcknowledgeMessage(this, SVarOwnerChangeInProgressMessage(msg.sender, msg.sVar, msg.newOwner))
  }

  addHandler[AcknowledgeMessage]{ msg =>
    msg.refMessage match {
      case SVarOwnerChangeInProgressMessage(oldSender, svar, newOwner) => heldMessages.get(svar) match {
        case Some(changeOwnerStatus) => changeOwnerStatus.acknowledgeBy( msg.sender )
        case None =>
      }
    }
  }

  addHandler[AcceptSVarMessage[_]]{ msg =>
    updateSVarOwner(msg.sVar, msg.sender)
    removeSVarData(msg.sVar)
    heldMessages.get(msg.sVar) match {
      case Some(changeOwnerStatus) => changeOwnerStatus.acceptChange()
      case None =>
    }
  }

  addHandler[HeldMessagesMessage]{ msg =>
    updateSVarOwner(msg.sVar, msg.newOwner)
    msg.msgs.foreach( applyHandlers( _ ) )

    //Handle msgs stored after the acknowledge was sent
    heldMessages.get(msg.sVar) collect {
      case changeOwnerStatus =>
        heldMessages.remove( msg.sVar )
        changeOwnerStatus(this).collect {
          case queue => queue.foreach( applyHandlers( _ ) )
        }
    }
  }

  addHandler[ChangeOwnerOfSVarMessage[_]]{ msg =>
    if (isOwnerChangeInProgress(msg.sVar))
      changeOwnerInProgessHandler( msg )
    else {
      val successful = changeOwner(msg.sender, msg.sVar, msg.newOwner)
      if (! successful) getLocallyKnownSVarOwner(msg.sVar) match {
        case Some(owner) => msg.sender ! SVarOwnerChangedMessage(this, msg.sVar, owner, msg)
        case _           => msg.sender ! UnknownSVarMessage(this, msg.sVar, msg)
      }
    }
  }

  addHandler[CreateSVarMessage[_]] { msg =>
    msg.sender ! SVarCreatedMessage( this, SVarImpl( msg.value ), msg.asInstanceOf[CreateSVarMessage[Any]], getManifest( msg.value ) )
  }

  addHandler[ReadSVarMessage[_]]{
    handleOwnerDependentMsg( msg => msg.sender ! createValueOfSVarMsg( msg.sVar ) )
  }

  addHandler[WriteSVarMessage[Any]]{
    handleOwnerDependentMsg( msg => write( msg.writer, msg.sVar, msg.value ) )
  }

  addHandler[UpdateSVarMessage[Any]]{
    handleOwnerDependentMsg( msg => update( msg.writer, msg.sVar, msg.updateMethod ) )
  }

  addHandler[ObserveSVarMessage]{
    handleOwnerDependentMsg( msg => addObserver(msg.sVar, msg.sender, msg.ignoredWriters) )
  }

  addHandler[IgnoreSVarMessage]{
    handleOwnerDependentMsg( msg => removeObserver(msg.sVar, msg.sender.asInstanceOf[SVarActorImpl]) )
  }

  addHandler[NotifyWriteSVarMessage[_]] { msg =>
    sVarObserveHandlers get msg.sVar collect { case handler => handler( msg.value ) }
  }

  addHandler[SVarOwnerChangedMessage[_]] { msg =>
    sVarOwners += msg.sVar -> msg.newOwner
    msg.newOwner ! msg.originalMessage
  }

  addHandler[BunchOfSirisMessagesMessage]{
    msg => msg.msgs.foreach( applyHandlers(_) )
  }
}
