package siris.ontology

import scala.collection.mutable.{SynchronizedMap, HashMap}
import siris.core.entity.description.{SVal, Semantics}
import siris.core.entity.component.Removability
import siris.core.entity.typeconversion._
import siris.core.entity.Entity
import reflect.ClassManifest

/* author: dwiebusch
 * date: 14.09.2010
 */

object SVarDescription {
  //! the registered components
  private val registry =
    new HashMap[Symbol, Set[ConvertibleTrait[_]]] with SynchronizedMap[Symbol, Set[ConvertibleTrait[_]]]

  /**
   *  retrieve an registered SVarDescription
   * @param typeinfo the typeinfo of the SVarDescription to be retrieved
   * @return the set of matching OntologyMembers
   */
  def apply( typeinfo : Symbol ) : Set[ConvertibleTrait[_]] =
    registry.getOrElse(typeinfo, Set())

  def apply( typeinfo : Symbol, semantics : Symbol ) : Option[ConvertibleTrait[_]] =
    apply(typeinfo).find( _.semantics.toSymbol == semantics )

  private[ontology] def apply[T]( o : ConvertibleTrait[T] ) : SVarDescription[T, T] =
    new SVarDescription[T, T](o.semantics, o.defaultValue(), o.typeinfo, o)

  private def register( c : ConvertibleTrait[_] ) {
    registry.update(Symbol(c.typeinfo.toString), registry.getOrElse(Symbol(c.typeinfo.toString), Set()) + c)
  }
}

class SVarDescription[T, B] private( val semantics : Semantics, newInstance : => T, val typeinfo : ClassManifest[T],
                                     protected val base : ConvertibleTrait[B] )
  extends ConvertibleTrait[T] with Serializable
{
  def this( that : SVarDescription[T, B] ) =
    this(that.semantics, that.defaultValue(), that.typeinfo, that.base)

  def createdBy[U](ctor : => U)(implicit newManifest : ClassManifest[U]) : SVarDescription[U, B] =
    new SVarDescription(semantics, ctor, newManifest, base)

  def as( newSemantics : SVal[Semantics] ) : SVarDescription[T, B] =
    as(newSemantics.value)

  def as( newSemantics : Semantics ) : SVarDescription[T, B] =
    new SVarDescription(newSemantics, newInstance, typeinfo, base)

  override def isProvided : Provide[T, B] =
    providedAs[B](base)

  override def isRequired : Require[B, T] =
    base.requiredAs[T](this)

  var annotations =
    Set[SVal[Semantics]]()

  def defaultValue() =
    newInstance

  SVarDescription.register(this)
}

protected abstract class EntitySVarDescription[T <: Entity] private( that     : SVarDescription[T, Entity],
                                                                     val ctor : Entity with Removability => T with Removability,
                                                                     manifest : ClassManifest[T]) extends SVarDescription(that){
  def this(symbol : SVal[Semantics], ctor : (Entity with Removability) => T with Removability)(implicit manifest : ClassManifest[T]) =
    this(types.Entity.as(symbol).createdBy(ctor(new Entity with Removability).asInstanceOf[T])(manifest), ctor, manifest)
}
