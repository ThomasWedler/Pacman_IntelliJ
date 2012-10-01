package siris.core.svaractor

import handlersupport.Types
import scala.actors.Actor
import scala.util.continuations._
import java.util.UUID
import reflect.ClassManifest

/**
 * User: stephan_rehfeld
 * Date: 03.08.2010
 * Time: 11:28:10
 */


object SVarImpl extends SVarObjectInterface {
  private implicit def getManifest[T]( v : T ) : (T, ClassManifest[T]) =
    (v, ClassManifest.fromClass(v.asInstanceOf[AnyRef].getClass).asInstanceOf[ClassManifest[T]])

  def applyC[T](value: T, owner: SVarActor): SVar[T] @Types.HandlerContinuation =
    if (Actor.self == owner)
      shiftUnit(apply(value))
    else {
      owner ! CreateSVarMessage( Actor.self, value )
      val retVal = SVarActorImpl.self.continueWith[SVarCreatedMessage[T], SVar[T]]{
        msg => msg.sVar.asInstanceOf[SVar[T]]
      }      
      retVal
    }

  //receive / react?
  def apply[T](value: T, owner: SVarActor): SVar[T] = {
    owner ! CreateSVarMessage( Actor.self, value )
    var returnValue : Option[SVar[T]] = None
    Actor.self receive {
      case SVarCreatedMessage( sender, sVar: SVar[_], createMessage, manifest) => returnValue = Some(sVar.asInstanceOf[SVar[T]])
    }
    returnValue.get
  }

  def apply[T](value: T) : SVar[T] =
    SVarActorImpl.self.createSVar(value)
}

class SVarImpl[T] private(initialOwner: SVarActor, override val id: UUID, override val containedValueManifest: ClassManifest[T]) extends SVar[T] {

  def this(initialOwner: SVarActor, man: ClassManifest[T]) = this(initialOwner, UUID.randomUUID, man)

  def owner( newOwner: SVarActor ) = {
    val currentActor = Actor.self
    val currentOwner = owner()

    if(currentActor == currentOwner)
      currentOwner.asInstanceOf[SVarActorImpl].changeOwner(currentActor, this, newOwner)
    else
      currentOwner ! ChangeOwnerOfSVarMessage( currentActor, this, newOwner )
  }

  def owner() : SVarActor = Actor.self match {
    case actor : SVarActorImpl => actor.getOrUpdateSVarOwner(this, initialOwner)
    case _ => initialOwner
  }

  def observe(ignoredWriters: Set[Actor])(handler: T => Unit) {
    if ( Actor.self == owner )
      SVarActorImpl.self.addObserver( this, SVarActorImpl.self, ignoredWriters )
    else
      owner ! ObserveSVarMessage( SVarActorImpl.self, this, ignoredWriters )

    SVarActorImpl.self.addSVarObserveHandler(this, handler.asInstanceOf[(Any => Unit)])
  }

  def observe(handler: T => Unit) {
    observe(Set[Actor]())(handler)
  }

  def ignore() {
    owner ! IgnoreSVarMessage( Actor.self, this )
    SVarActorImpl.self.removeSVarObserveHandlers( this )
  }

  def get( consume: (T) => Unit ) {
    val self = SVarActorImpl.self
    if( self == owner )
      consume ( self.read( this ) )
    else {
      owner ! ReadSVarMessage( self, this )
      self.addSingleUseHandler[ValueOfSVarMessage[T]]( {
        case ValueOfSVarMessage(_, svar, value) if (svar == this) => consume(value.asInstanceOf[T])
      } : PartialFunction[ValueOfSVarMessage[T], Unit])
    }
  }

  def get : T @Types.HandlerContinuation = {
    val self = SVarActorImpl.self
    if (owner == self)
      shiftUnit( self.read( this ) )
    else {
      owner ! ReadSVarMessage( self, this )
      self.continueWith[ValueOfSVarMessage[T], T]{
        case ValueOfSVarMessage(_, svar, value) if (svar == this) => value.asInstanceOf[T]
      }
    }
  }

  def set( value: T ) {
    val self = Actor.self
    if (owner == self)
      SVarActorImpl.self.write(self, this, value)
    else
      owner ! WriteSVarMessage( self, self, this, value )
  }

  def update( updateMethod : T => T ){
    if (owner == Actor.self){
      val self = SVarActorImpl.self
      self.update( self, this, updateMethod )
    } else {
      var self = Actor.self
      owner ! UpdateSVarMessage(self, self, this, updateMethod)
    }

  }
}