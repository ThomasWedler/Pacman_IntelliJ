package siris.components.renderer.jvr

import siris.ontology.SVarDescription

/**
 * Created by IntelliJ IDEA.
 * User: stephan_rehfeld
 * Date: 12.09.11
 * Time: 09:38
 * To change this template use File | Settings | File Templates.
 */

abstract class UniformListContaining[T <: UniformListContaining[T] ] {
  var uniformList : List[UniformManager[_,T]]
}

// generic to work with render pass and postprocessing effect.
class UniformNameHolder[P <: UniformListContaining[P] ]( name: String, parent : P ) {
  def hasValue[T]( value : T ) = new UniformManager( name, value, None, parent )
}

// generic to work with render pass and postprocessing effect.
class UniformManager[T,P <: UniformListContaining[P] ]( val name: String, val value : T, var ontologyMember : Option[ SVarDescription[T, _] ], parent : P ) {
  val one : List[UniformManager[_,P]] = this :: Nil

  parent.uniformList = parent.uniformList ::: one

  def isReachableBy( om : SVarDescription[T, _] ) : UniformManager[T,P] = {
  ontologyMember = Some( om )
    this
  }

  def and( name : String ) : UniformNameHolder[P] = {
    new UniformNameHolder( name, parent )
  }
  def pack : P = parent
}