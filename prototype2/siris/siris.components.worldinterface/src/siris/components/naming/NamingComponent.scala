package siris.components.naming

import siris.core.component.Component
import siris.core.svaractor.SVarActorLW
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.entity.description._
import siris.core.entity.Entity
import siris.ontology.Symbols
import siris.ontology.types.Name
import siris.core.entity.component.{EntityConfigLayer, Removability}

/**
 * User: dwiebusch
 * Date: 13.04.11
 * Time: 15:42
 */

object NamingAspect{
  private val symbol = siris.ontology.types.OntologySymbol('naming)
  def apply() : SVal[Semantics] = symbol
}

case class NameIt(name : String) extends Aspect(NamingAspect(), Symbols.name, Nil){
  def getFeatures = Set(Name)
  NamingComponent

  protected def getCreateParams =
    addCVars(Name(name) :: Nil)

  def getProvidings =
    getCreateParams.map(_.typedSemantics).toSet
}

object NamingComponent{
  (new NamingComponent).start()

  def giveName(name : String) : EntityAspect =
    NameIt(name).toEntityAspect
}

protected trait NamingComponentTrait extends Component with EntityConfigLayer{
  def componentName = NamingAspect().value.toSymbol
  def componentType = NamingAspect()
}

private class NamingComponent protected() extends SVarActorLW with NamingComponentTrait{
  protected def removeFromLocalRep(e: Entity) {}
  override protected def entityConfigComplete(e: Entity with Removability, aspect: EntityAspect) {}

  protected def configure(params: SValList) = null

  override protected def requestInitialValues(toProvide: Set[ConvertibleTrait[_]],
                                          aspect: EntityAspect, e: Entity, given: SValList) {
    provideInitialValues(e, aspect.createParamSet.combineWithValues(toProvide)._1)
  }

  override protected def getDependencies(aspect: EntityAspect) =
    super.getDependencies(aspect) ++ Set(Dependencies(Providing(Name)))

  override protected def getAdditionalProvidings(aspect: EntityAspect) =
    Set()
}