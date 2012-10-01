package siris.components.sound

import siris.core.entity.description.Semantics
import siris.ontology.types.OntologySymbol

/**
 * User: dwiebusch
 * Date: 16.06.11
 * Time: 11:26
 */

object SoundSymbols {
  //private class SemanticSymbol(val toSymbol: Symbol) extends Semantics
  //private object SemanticSymbol{ def apply( s : Symbol ) : Semantics = new SemanticSymbol(s) }

  val soundObjects = OntologySymbol('soundObjects)
  val soundListener = OntologySymbol('soundListener)
  val soundFile  = OntologySymbol('soundFile)
  val onCreation  = OntologySymbol('onCreation)
  val onCollision  = OntologySymbol('onCollision)
  val onEvent  = OntologySymbol('onEvent)
  val onEventList = OntologySymbol('onEventList)
  var soundEvents = OntologySymbol('soundEvents)

  def water(n: Int) = OntologySymbol(Symbol("water"+n))

  val explosion = OntologySymbol('explosion)
  val hitByIce = OntologySymbol('hitByIce)
  val hitByFlame = OntologySymbol('hitByFlame)
  val shieldDisabled = OntologySymbol('shieldDisabled)
  val shieldEnabled = OntologySymbol('shieldEnabled)
  val wellLeft = OntologySymbol('wellLeft)
  val playerStartMoving = OntologySymbol('playerStartMoving)
  val playerStopMoving = OntologySymbol('playerStopMoving)
  val playerJumped = OntologySymbol('playerJumped)
  val playerHit = OntologySymbol('playerHit)
  val wellApproached = OntologySymbol('wellApproached)
  val notEnoughMana = OntologySymbol('notEnoughMana)
  val beginIceCharge = OntologySymbol('beginIceCharge)
  val endIceCharge = OntologySymbol('endIceCharge)
  val beginFireCharge = OntologySymbol('beginFireCharge)
  val endFireCharge = OntologySymbol('endFireCharge)
  val alarm = OntologySymbol('alarm)
  val playerLost = OntologySymbol('playerLost)
  val playerWon = OntologySymbol('playerWon)

  val beginOfInteraction = OntologySymbol('beginOfInteraction)
  val endOfInteraction = OntologySymbol('endOfInteraction)
}