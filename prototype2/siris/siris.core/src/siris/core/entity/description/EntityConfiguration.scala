package siris.core.entity.description

import siris.core.entity.Entity
import scala.Function._

/**
 * @author dwiebusch
 * Date: 22.06.11
 * Time: 15:51
 */

case class EntityConfiguration(e: Entity, csets: Map[Symbol, NamedSValList]) {
  override def toString: String = {
    "EntityConfiguration: \n" +
      csets.toList.map(tupled(
        (s: Symbol, cps: NamedSValList) =>
          if(cps != null) {          "\tComponent:\t" + s.toString + "\n\tAspect:\t\t\t" + cps.semantics + "\n" +
            cps.toList.map {
              cp: SVal[_] =>
                "\t\t" + cp.typedSemantics.sVarIdentifier.toString + " = " + cp.toString + "\n"
            }.foldLeft("")(_ + _)}
          else {"\tComponent:\t" + s.toString + "\n\tAspect:\t\t\t" + "Null"}
      )).foldLeft("")(_ + _)
  }
}