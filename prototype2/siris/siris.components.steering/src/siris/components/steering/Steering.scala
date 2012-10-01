package siris.components.steering

import siris.core.component.Component
import siris.core.entity.description._
import siris.core.entity.Entity
import actors.Actor
import siris.core.svaractor.{SVarActorHW, SVar}
import siris.core.helper.TimeMeasurement


/**
 * User: stephan_rehfeld
 * Date: 21.09.2010
 * Time: 10:55:09
 */

//@deprecated( "Use new steering framework" )
case class CalculateSteering( sender: Actor )

//@deprecated( "Use new steering framework" )
class Steering( val componentName: Symbol ) extends SVarActorHW with Component with TimeMeasurement {

  override def WakeUpMessage: Any = CalculateSteering(this)
  protected def configure(params: SValList) = null

  def componentType = siris.ontology.types.OntologySymbol(componentName)

  //@deprecated( "Use new steering framework" )
  var controls : List[(Long) => Unit] = List()
  //@deprecated( "Use new steering framework" )
  var lastTime : Long = 0

  addHandler[CalculateSteering] {
    case CalculateSteering( sender ) =>
     if( lastTime != 0 ) {
       val timeDiff = System.nanoTime - lastTime
       for( control <- controls ) control( timeDiff )
     }

     lastTime = System.nanoTime
     requestWakeUpCall(Period(10L))
  }

  addHandler[Controller]( {
    case Controller( controller, normalizer, controlledObject, mover, map ) =>
      var keys : Set[Symbol] = Set()
      for( (key,_) <- map ) {
        keys = keys + key
      }
      normalizer.config( controller, keys )
      def controllFunction = mover.control( controlledObject, sVarReaderNormalizerAndCorrectNameProvider( controller, normalizer, map ), _ : Long ) 
      controls = controls :+ controllFunction
  })

  protected def removeFromLocalRep(e: Entity) {}

  def sVarReaderNormalizerAndCorrectNameProvider( controller : Entity, normalizer : ControllerNormalizer, map : List[(Symbol,Symbol)] ) : Map[Symbol,Float] = {
    var m : Map[Symbol,Float] = Map()
    for( (source,target) <- map ) {
      m = m + (target -> normalizer.getNormalized( source ) )
    }
    m
  }

  //@deprecated( "Use new steering framework" )
  protected def handleNewSVar[T](sVarName: Symbol, sVar: SVar[T], e: Entity, cparam: NamedSValList) {

  }

  //@deprecated( "Use new steering framework" )
  protected def entityConfigComplete(e: Entity, cParam: NamedSValList) {

  }
}