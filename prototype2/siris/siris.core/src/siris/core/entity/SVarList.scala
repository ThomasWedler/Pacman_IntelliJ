package siris.core.entity

import description.{Semantics, SVal}
import siris.core.svaractor.SVar

/**
 * @author dwiebusch
 * Date: 19.09.11
 * Time: 14:59
 */

case class SVarList[T] protected[entity] ( in : List[SVarContainer[T]] ) {
  def filter( os : SVal[Semantics] ) : SVarList[T] =
    new SVarList( in.filter( _.annotations.contains(os) ) )

  def execIfUnique( f : SVar[T] => Any ) {
    if (in.size == 1)
      f(in.head.svar)
  }

  def mapExecution( f : SVar[T] => Any) {
    in.foreach( c => f( c.svar ) )
  }

  def size =
    in.size

  def getUnannotated : List[SVar[T]] =
    in.filter( _.annotations.isEmpty ).map( _.svar )

  protected[entity] def map[U]( f : SVarContainer[T] => SVarContainer[U] ) : SVarList[U] =
    SVarList( in.map(f) )
}