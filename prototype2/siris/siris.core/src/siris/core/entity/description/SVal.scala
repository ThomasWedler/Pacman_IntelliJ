package siris.core.entity.description

import scala.collection.mutable.MutableList
import scala.reflect.ClassManifest._
import siris.core.entity.typeconversion.{Reverter, Converter, ConvertibleTrait}

/**
 *
 * This class represents a value with a semantics.
 */
final case class SVal[T](typedSemantics: ConvertibleTrait[T])(_value: T) {
  private val baseValue = Converter(typedSemantics, typedSemantics.getBase).convert(_value)
  /**
   *
   * Returns the value of this CreateParam in the given type.
   *
   * @details
   * This method searches and calls a IReverter if needed.
   * If no suitable IReverter is found a NoReverterFoundException is thrown.
   */
  def as[O](outputHint: ConvertibleTrait[O]): O =
    Reverter(outputHint, typedSemantics.getBase ).revert(baseValue)

  /**
   * The type of the contained value
   */
  type typeOfContainedValue = typedSemantics.dataType

  //! the typeinfo of the type of the contained value
  val containedValueManifest = getManifest(value)

  /**
   * Returns the value containd in this CreateParameter
   */
  def value = _value

  /**
   *
   * Overwritten equals method to use OntologyMembers in collections.
   *
   * @details
   * This method was implemented using the guidline from "Programming in Scala" chapter 28.
   *
   * @see Programming in Scala - Odersky, Spoon, Venners - First Edition, Version 6
   */
  override def equals(other: Any): Boolean = other match {
    case that: SVal[_] => typedSemantics.equals(that.typedSemantics) && value.equals(that.value)
    case _             => false
  }

  /**
   *
   * Overwritten hashCode method to use OntologyMembers in collections.
   *
   * @details
   * This method was implemented using the guidline from "Programming in Scala" chapter 28.
   *
   * @see Programming in Scala - Odersky, Spoon, Venners - First Edition, Version 6
   */
  override def hashCode: Int =
    41 * ( 41 + typedSemantics.hashCode() )  + value.hashCode()
  //41 * ( 41 + typedSemantics.typeinfo.hashCode ) + typedSemantics.sVarIdentifier.hashCode

  override def toString: String = "SVal(" + typedSemantics.typeinfo.erasure.getSimpleName + " as " +
    typedSemantics.semantics + ") = " +  value.toString

  def ::(that : SVal[_]) : List[SVal[_]] =
    this :: that :: Nil

  def and(that : SVal[_]) : SValSeq =
    new SValSeq and this and that

  def and : Seq[SVal[_]] => SValSeq =
    _.foldLeft(new SValSeq)( _ and _ )

  protected def getManifest[T]( value : T ) : ClassManifest[_] =
    fromClass(value.asInstanceOf[AnyRef].getClass)
}

class SValSeq ( cvar : SVal[_]* ) extends Seq[SVal[_]]{
  private var internalList = List[SVal[_]](cvar : _*)
  def iterator = internalList.iterator
  def apply(idx: Int) = internalList(idx)
  def length = internalList.length
  def and( cvar : SVal[_] ) : SValSeq = { internalList = cvar :: internalList; this }
}


/**
 *
 *
 * This exception is thrown if a NamedSValList can not find a requested CreateParam.
 *
 * @param
 * The sVarIdentifier of the CreateParam that was not found.
 */
case class SValNotFound(sVarIdentifier: Symbol) extends Exception(sVarIdentifier.name)

/* author: igg.dwiebusch, igg.mfischbach :P
 * date: 27.08.2010
 */

abstract class Semantics {
  def toSymbol: Symbol
  override def toString: String = toSymbol.name
}


object SValList {

  /**
   *
   * Creates a new SValList
   */
  def apply(params: SVal[_]*) = new SValList(params:_*)

  /**
   *
   * Creates a new SValList that is a duplicate of that
   */
  def apply(that: SValList) = new SValList(that)
}

/**
 *
 * Groups several CreateParams using a LinkedList.
 *
 * @see EntityAspect
 */
//TODO: make constructors package private to force companion object usage
class SValList(params: SVal[_]*) extends MutableList[SVal[_]] {
  params.foreach{this.+=(_)}

  /**
   *
   * Creates a new SValList that is a duplicate of that
   */
  def this(that: SValList) = this(that.toSeq:_*)

  /**
   *
   * Returns a NEW SValList that does not contain any OntologyMember of type typedSemantics.
   */
  def -[T](typedSemantics: ConvertibleTrait[T]) =
    new SValList(filter(_.typedSemantics != typedSemantics).toSeq:_*)

  /**
   *
   * Returns a List of all included CreateParams that are of a sepcific OntologyMember type.
   */
  def getAllCreateParamsFor[T](typedSemantics: ConvertibleTrait[T]): List[SVal[T]] =
    filter(_.typedSemantics == typedSemantics).map(_.asInstanceOf[SVal[T]]).toList

  /**
   *
   * Returns an Option on the CreateParam of a specific OntologyMember type.
   * Returns the first CreateParam in the order of this SValList if more than one is present.
   */
  def getFirstCreateParamFor[T](typedSemantics: ConvertibleTrait[T]): Option[SVal[T]] =
    getAllCreateParamsFor(typedSemantics).headOption

  /**
   *
   * Returns the CreateParam of a specific OntologyMember type.
   * If the requested CreateParam is not found a SValNotFound is thrown.
   * If more than one valid CreateParam is present, the first CreateParam in the order of this SValList is returned.
   */
  def firstCreateParamFor[T](typedSemantics: ConvertibleTrait[T]): SVal[T] =
    getFirstCreateParamFor(typedSemantics).getOrElse( throw(SValNotFound(typedSemantics.sVarIdentifier)) )

  /**
   *
   * Tests if this SValList contains at least one CreateParam of the given OntologyMember type.
   */
  def containsCreateParam[T](typedSemantics: ConvertibleTrait[T]): Boolean =
    contains(SVal(typedSemantics)(typedSemantics.defaultValue()))

  /**
   *  Adds the given SVal if no other CreateParam of its OntologyMember type
   *        was contained before.
   */
  def addIfNew(createParam: SVal[_]): this.type = {
    //if(!contains(createParam.typedSemantics)) this.+=(createParam); this }
    if( forall(!_.typedSemantics.equals( createParam.typedSemantics) ) ) this.+=(createParam); this }


  /**
   *
   * Returns a List of all included values that are of a specific OntologyMember type.
   */
  def getAllValuesFor[T](typedSemantics: ConvertibleTrait[T]): List[T] =
    getAllCreateParamsFor(typedSemantics).map(_.as(typedSemantics))

  /**
   *
   * Returns an Option on the value of a specific OntologyMember type.
   * Returns the first value in the order of this SValList if more than one is present.
   */
  def getFirstValueFor[T](typedSemantics: ConvertibleTrait[T]): Option[T] =
    getAllValuesFor(typedSemantics).headOption

  /**
   *
   * Returns the value of a specific OntologyMember type.
   * If the requested OntologyMember type is not found defaultValue is returned.
   * If more than one valid value is present, the first value in the order of this SValList is returned.
   */
  def getFirstValueForOrElse[T](typedSemantics: ConvertibleTrait[T])(defaultValue: T): T =
    getFirstValueFor(typedSemantics).getOrElse(defaultValue)

  /**
   *
   * Returns the value of a specific OntologyMember type.
   * If the requested CreateParam is not found a SValNotFound is thrown.
   * If more than one valid value is present, the first value in the order of this SValList is returned.
   */
  def firstValueFor[T](typedSemantics: ConvertibleTrait[T]): T =
    getFirstValueFor(typedSemantics).getOrElse( throw(SValNotFound(typedSemantics.sVarIdentifier)) )

  /**
   *
   * Determines if this SValList contains at least one CreateParam for each Providing
   */
  def satisfies(p: Set[Providing]): Boolean =
    p.flatMap(_.objects).foldLeft(true)((sat, p) => sat && containsCreateParam(p))

  /**
   *
   * Inserts all CreateParms of that into this SValList
   */
  def mergeWith(that: SValList): this.type = {that.foreach(this.+=); this}

  /**
   *
   * Inserts all new CreateParams of that into this SValList
   */
  def xMergeWith(that: SValList): this.type = {that.foreach(this.addIfNew); this}

  /**
   *  combines the given convertible trait with the first matching value from this createparamset.
   * @param c the convertibletrait to be combined with a value
   * @return an option containing the created cvar, or None if no matching value was found
   */
  def combineWithValue[T](c : ConvertibleTrait[T]) : Option[SVal[T]] =
    getFirstValueFor(c) collect { case value => c(value) }

  /**
   *  combines all convertibles with their matching values in this createparamset.
   * @param cs the convertible trait to be matched
   * @return a tuple consisting of an SValList containing the created CVars and a set of convertible traits
   *         for which no value was found
   */
  def combineWithValues(cs: Set[ConvertibleTrait[_]]): (SValList, Set[ConvertibleTrait[_]]) =
    cs.foldLeft((new SValList, cs.empty)) {
      (tuple, elem) => combineWithValue(elem) match {
        case Some(cvar) => (tuple._1 += cvar, tuple._2)
        case None => (tuple._1, tuple._2 + elem)
      }
    }

  override def toString(): String =
    "SValList\n\t" + mkString("\n\t")
}

/**
 *
 * Groups several CreateParams using a HashMap.
 * This is used in the entity creation process for example.
 *
 * @see EntityAspect, SValList
 */
final case class NamedSValList(val semantics: SVal[Semantics], params: SVal[_]*) extends SValList(params: _*) {
  def this(that: NamedSValList) = this(that.semantics, that.toSeq:_*)
  def this(aspectType: SVal[Semantics], cps: SValList) = this(aspectType, cps.toSeq:_*)

  /**
   *
   * Returns a NEW NamedSValList that does not contain any OntologyMember of type typedSemantics.
   */
  override def -[T](typedSemantics: ConvertibleTrait[T]) =
    new NamedSValList(semantics, filter(_ != SVal(typedSemantics)(typedSemantics.defaultValue())).toSeq:_*)

  override def toString(): String =
    "NamedSValList (" + semantics + ")\n\t" + mkString("\n\t")
}