package siris.applications.basicexample.component

import siris.ontology.Symbols
import siris.ontology.types.{Transformation, Identifier}
import siris.core.entity.description.{SVal, NamedSValList, Semantics, Aspect}

/**
 * @author dwiebusch
 * Date: 01.07.11
 * Time: 16:45
 */

abstract class ExampleAspect( aspectType : SVal[Semantics], targets : List[Symbol] = Nil )
  extends Aspect(Symbols.interface, aspectType, targets)

case class ExampleSphereAspect(id : Symbol) extends ExampleAspect(Symbols.sphere){
  def getFeatures   = Set(Identifier, Transformation)
  def getProvidings = Set(Identifier)

  protected def getCreateParams =
    NamedSValList(aspectType, Identifier(id))
}