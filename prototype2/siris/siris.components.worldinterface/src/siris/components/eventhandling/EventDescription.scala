package siris.components.eventhandling

import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.entity.Entity
import siris.core.entity.description.{SValList, Semantics, SVal}

/* author: dwiebusch
 * date: 03.12.2010
 */


class Event protected[eventhandling](val name : SVal[Semantics], values : SValList = new SValList(), val affectedEntities : Set[Entity] = Set())
extends Serializable {
  def get[T]( c : ConvertibleTrait[T] ) : Option[T] =
    values.getFirstValueFor(c)

  def getAll[T]( c : ConvertibleTrait[T] ) : List[T] =
    values.getAllValuesFor(c)
}

class EventDescription(val name : SVal[Semantics], val restriction : PartialFunction[Event, Boolean] = {case e : Event => true})
extends Serializable {
  def matches( e : Event ) : Boolean =
    restriction(e)

  def createEvent(affectedEntities : Set[Entity], values : SVal[_]* ) : Event =
    new Event(name, new SValList(values:_*), affectedEntities)

  def createEvent( values : SVal[_]* ) =
    apply(values :_*)

  def apply(values : SVal[_]* ) : Event =
    apply(Set[Entity](), values:_*)

  def apply( affectedEntities : Set[Entity], values : SVal[_]* ) : Event =
    createEvent(affectedEntities, values:_*)

  def restrictedBy(restr: PartialFunction[Event, Boolean]) : EventDescription =
    new EventDescription(name, restr)
}

object DefaultRestrictions{
  def containsEntity(ent : Entity) : PartialFunction[Event, Boolean] = {
    case (e : Event) => e.affectedEntities.contains(ent)
  }

  def containsEntities(set : Set[Entity]) : PartialFunction[Event, Boolean] = {
    case (e : Event) => set.forall( e.affectedEntities.contains(_) )
  }
}