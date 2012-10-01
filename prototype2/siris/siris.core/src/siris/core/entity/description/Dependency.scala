package siris.core.entity.description

import siris.core.entity.typeconversion.ConvertibleTrait
import collection.mutable.HashSet

/**
 * User: dwiebusch
 * Date: 06.05.11
 * Time: 14:37
 */

case class Providing( objects : ConvertibleTrait[_]* )

case class Requiring( objects : ConvertibleTrait[_]* )

case class Dependencies( providings : Providing, requirings : Requiring )

object Dependencies{
  def apply( p : Providing ) : Dependencies =
    Dependencies(p, Requiring())

  def apply( r : Requiring ) : Dependencies =
    Dependencies(Providing(), r)
}

object DependenciesSet {
  //This needs no include. The compiler looks by default in companion object
  implicit def toSet(dSet: DependenciesSet): Set[Dependencies] = dSet.toSet
}
class DependenciesSet extends HashSet[Dependencies] {
  def += (r: Requiring): HashSet[Dependencies] = this.+=(Dependencies(r))
  def += (p: Providing): HashSet[Dependencies] = this.+=(Dependencies(p))
  def += (providings : Providing, requirings : Requiring): HashSet[Dependencies] = this.+=(Dependencies(providings, requirings))
}