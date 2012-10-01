package siris.core.svaractor.synclayer

import siris.core.svaractor._
import scala.collection.mutable
import actors.Actor

/* author: dwiebusch
 * date: 03.09.2010
 */

trait PluggableSyncLayer extends SVarActorImpl with SVarSyncLayer{
  private val synchLists      = mutable.Map[SVarActor, List[SVarMessage]]()
  private val synchPairs      = mutable.Map[SVar[_], List[SVarActor]]()
  private val observers       = mutable.Set[SVarActor]()
  private val storedObservers = mutable.Map[SVarActor, Map[SVar[_], Set[Actor]]]()

  addHandler[ObserveMessage]{ msg => observe( msg.sender ) }
  addHandler[IgnoreMessage] { msg => ignore ( msg.sender ) }
  
  protected def push() {
    synchLists.foreach( pair => pair._1 ! BunchOfSirisMessagesMessage(this, (SyncMessage(this) :: pair._2).reverse ))
    synchLists.clear()
  }

  def ignore() {
    if (this == SVarActorImpl.self)
      ignore(SVarActorImpl.self)
    else
      this ! IgnoreMessage( SVarActorImpl.self )
  }


  def observe() {
    if (this == SVarActorImpl.self)
      observe( SVarActorImpl.self )
    else
      this ! ObserveMessage( SVarActorImpl.self )
  }

  override protected def internalWrite[T](writer: Actor, sVar : SVar[T], value: T) {
    super.internalWrite(writer, sVar, value)
    synchedWrite( writer, sVar, value )
  }

  override protected def internalAddObserver( sVar: SVar[_], a: SVarActor, ignoredWriters: Set[Actor] ) {
    if ( observers.contains(a)) synchPairs.get(sVar) match {
      case Some(list) => synchPairs.update(sVar, a :: list)
      case None => synchPairs.put( sVar, a :: Nil )
    }
    else
      super.internalAddObserver( sVar, a, ignoredWriters )
  }

  override protected def internalRemoveObserver(sVar: SVar[_], a: SVarActor) {
    if (getSVarsObservedBy(a).contains(sVar))
      super.internalRemoveObserver(sVar, a)
    else {
      synchPairs.update(sVar, synchPairs.getOrElse(sVar, Nil).filterNot( _ == a))
      synchLists.update(a, synchLists.getOrElse(a, Nil).filterNot( _.asInstanceOf[NotifyWriteSVarMessage[Any]].sVar == sVar))
    }
  }

  override protected def moveObservers[T]( svar : SVar[T], newOwner : SVarActor) : List[SIRISMessage] = {
    var retVal = List[SIRISMessage]()
    synchPairs.remove(svar).getOrElse(Nil).foreach( actor => {
      actor ! BunchOfSirisMessagesMessage(this, synchLists.remove(actor).getOrElse(Nil))
      retVal = ObserveMessage( actor ) :: retVal
    })
    super.moveObservers(svar, newOwner) ::: retVal
  }


  private def synchedWrite[T]( writer: Actor, sVar : SVar[T], value : T ) {
    synchPairs.getOrElse(sVar, Nil).foreach{
      actor => if (!storedObservers.getOrElse(actor, Map()).getOrElse(sVar, Set()).contains(writer))
        synchLists.put(actor, NotifyWriteSVarMessage(this, sVar, value) :: synchLists.getOrElse(actor, Nil))
    }
  }


  private def observe( observer : SVarActor) {
    observers += observer
    getSVarsObservedBy(observer).foreach( pair => {
      synchPairs.put( pair._1, observer :: synchPairs.getOrElse( pair._1, Nil) )
      val toUpdate = storedObservers.getOrElseUpdate(observer, Map())
      storedObservers.update(observer, toUpdate.updated(pair._1, getObserversOf(pair._1).getOrElse(observer, Set())))
      removeObserver( pair._1, observer )
    })
  }

  private def ignore( actor : SVarActor ){
    observers -= actor
    actor ! BunchOfSirisMessagesMessage(actor, synchLists.remove(actor).getOrElse(Nil) )
    synchPairs.filter( _._2.contains( actor ) ).foreach{
      pair => {
        addObserver( pair._1, actor, storedObservers.getOrElse(actor, Map()).getOrElse(pair._1, Set()))
        synchPairs.update( pair._1, pair._2.filterNot( _ == actor ) )
      }
    }
  }

}