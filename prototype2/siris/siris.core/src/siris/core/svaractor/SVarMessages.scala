/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package siris.core.svaractor

import scala.actors.Actor


/*
 * @todo DOCUMENT THIS FILE
 */
abstract class SIRISMessage {
  def sender: Actor
}

abstract class SVarMessage extends SIRISMessage

abstract class SVarHoldingMessage extends SVarMessage{
  def sVar : SVar[_]
}

case class AcknowledgeMessage( sender: Actor, refMessage: SVarMessage ) extends SVarMessage

case class CreateSVarMessage[T]( sender: Actor, value: T ) extends SVarMessage
case class SVarCreatedMessage[T]( sender: Actor, sVar: SVar[T], createMessage: CreateSVarMessage[T], manifest : ClassManifest[_]) extends SVarHoldingMessage

case class ReadSVarMessage[T]( sender: Actor, sVar: SVar[T] ) extends SVarHoldingMessage
case class ValueOfSVarMessage[T]( sender: Actor, sVar: SVar[T], value: T ) extends SVarMessage

case class WriteSVarMessage[T]( sender: Actor, writer: Actor, sVar: SVar[T], value: T ) extends SVarHoldingMessage
case class UpdateSVarMessage[T]( sender: Actor, writer: Actor, sVar: SVar[T], updateMethod: T => T ) extends SVarHoldingMessage

case class ObserveSVarMessage( sender: SVarActor, sVar: SVar[_], ignoredWriters: Set[Actor] ) extends SVarHoldingMessage
case class IgnoreSVarMessage( sender: Actor, sVar: SVar[_] ) extends SVarHoldingMessage
case class NotifyWriteSVarMessage[T]( sender: Actor, sVar: SVar[T], value: T ) extends SVarHoldingMessage

case class ChangeOwnerOfSVarMessage[T](  sender: Actor, sVar: SVar[T], newOwner: SVarActor ) extends SVarHoldingMessage
case class OfferSVarMessage[T](  sender: SVarActor, sVar: SVar[T], value: T ) extends SVarHoldingMessage
case class AcceptSVarMessage[T](  sender: SVarActor, sVar: SVar[T] ) extends SVarHoldingMessage
case class SVarOwnerChangeInProgressMessage(  sender: SVarActor, sVar: SVar[_], newOwner : SVarActor ) extends SVarHoldingMessage
case class HeldMessagesMessage( sender: Actor, sVar : SVar[_], newOwner : SVarActor, msgs : List[SVarMessage]) extends SVarHoldingMessage

case class SVarOwnerChangedMessage[T]( sender: Actor, sVar: SVar[_], newOwner: SVarActor, originalMessage: SVarMessage ) extends SVarHoldingMessage

case class UnknownSVarMessage[T]( sender: Actor, sVar: SVar[T], originalMessage: SVarMessage ) extends SVarHoldingMessage


case class Shutdown( sender: Actor ) extends SIRISMessage

case class ObserveMessage( sender: SVarActor ) extends SIRISMessage
case class IgnoreMessage( sender: SVarActor ) extends SIRISMessage
case class SyncMessage( sender: SVarActor ) extends SIRISMessage
case class BunchOfSirisMessagesMessage( sender : SVarActor, msgs : List[SIRISMessage]) extends SIRISMessage



class SVarMessages {

}
