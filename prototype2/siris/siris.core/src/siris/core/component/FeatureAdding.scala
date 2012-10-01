package siris.core.component

import siris.core.svaractor.SVar
import siris.core.entity.Entity
import siris.core.entity.description.{SVal, Semantics, NamedSValList}

/**
 * Created by IntelliJ IDEA.
 * User: dwiebusch
 * Date: 11.02.11
 * Time: 14:23
 */

abstract class ComponentFeature( val name : SVal[Semantics]) {
  def svarHandling : (Symbol, SVar[_], Entity, NamedSValList, Component) => Unit
  def entityHandling : (Entity, NamedSValList, Component) => Unit
}

trait FeatureAdding extends Component{
  protected var featureMap = Map[SVal[Semantics], ComponentFeature]()

  protected def entityConfigComplete(e: Entity, cParam: NamedSValList) =
    featureMap.get(cParam.semantics).collect{
      case feature => feature.entityHandling(e, cParam, this)
    }

  protected def handleNewSVar[T](sVarName: Symbol, sVar: SVar[T], e: Entity, cparam: NamedSValList) =
    featureMap.get(cparam.semantics).collect{
      case feature => feature.svarHandling(sVarName, sVar, e, cparam, this)
    }

  def addFeature( f : ComponentFeature ) {
    featureMap = featureMap + (f.name -> f)
  }

  def removeFeature( name : Semantics ) {
    featureMap = featureMap.filterNot( _._1 == name )
  }
}

