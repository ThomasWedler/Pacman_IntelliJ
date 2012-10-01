package siris.ontology.types

import reflect.ClassManifest
import siris.ontology.{SVarDescription, Symbols}
import siris.core.entity.description.{SVal, Semantics}
import siris.core.entity.typeconversion.ConvertibleTrait

/**
 * User: dwiebusch
 * Date: 11.04.11
 * Time: 17:09
 */

//Base class for generated OntologyMembers
object NullType{
  def as( semantics : SVal[Semantics] ) = new NullType(semantics)
  def as( semantics : Semantics ) = new NullType(semantics)
}

class NullType private(name : Semantics){
  private def this( name : SVal[Semantics] = Symbols.nullType) = this(name.value)
  def createdBy[U]( ctor : => U )( implicit info : ClassManifest[U] ) = SVarDescription[U](new ConvertibleTrait[U]{
    protected val base = this
    def defaultValue() = ctor
    def semantics      = name
    def typeinfo       = info
    val annotations    = Set[SVal[Semantics]]()
  })
}

object OntologySymbolBase extends Semantics{ def toSymbol = 'OntologySymbol }
object OntologySymbol extends SVarDescription[Semantics, Semantics](NullType as OntologySymbolBase createdBy OntologySymbolBase){
  def apply(s : Symbol) = new SVal(this)(new Semantics{ def toSymbol = s })
}
