package siris.applications.basicexample.component

import siris.core.entity.description.{EntityAspect, SValList}
import siris.core.entity.Entity
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.svaractor.SVarActorHW
import siris.core.component.Component
import siris.ontology.Symbols
import siris.ontology.types.Identifier
import siris.core.entity.component.{EntityConfigLayer, Removability}

class ExampleComponent extends SVarActorHW with Component with EntityConfigLayer {
  // define component type and name
  def componentType = Symbols.interface
  def componentName = 'ExampleInterfaceComponent

  override protected def entityConfigComplete(e: Entity with Removability, aspect: EntityAspect) {
    // Your code goes here
    e.get(Identifier).collect{
      case svar => svar.get( value => println("Example component got entity with id \"" + value.name + "\""))
    }
  }

  override protected def requestInitialValues(toProvide: Set[ConvertibleTrait[_]], aspect: EntityAspect, e: Entity, given: SValList){
    provideInitialValues(e, aspect.createParamSet.combineWithValues(toProvide)._1)
  }


  // some methods which are not needed for now
  protected def configure(params: SValList)   {}
  protected def removeFromLocalRep(e: Entity) {}
  override protected def getAdditionalProvidings(aspect: EntityAspect)                      = Set()
}