package siris.core.entity

import description.{Semantics, SVal}
import siris.core.svaractor.SVar
import typeconversion._
import java.util.UUID

/* author: dwiebusch
 * date: 27.08.2010
 */

/**
 *  the entity class
 */
class Entity protected[entity]( private val sVars : Map[Symbol, List[SVarContainer[_]]], val id : UUID ){
  /**
   *  Copy-C'tor
   */
  protected def this( e : Entity ) =
    this(e.sVars, e.id)

  /**
   *  creates an empty entity
   */
  def this() =
    this(Map[Symbol, List[SVarContainer[_]]](), UUID.randomUUID)

  /**
   *  Returns the sVarNames of all SVars of this Entity
   */
  def getAllSVarNames =
    sVars.keySet

  def getAllSVars : Iterable[Tuple3[Symbol, ConvertibleTrait[_], SVar[_]]] =
    sVars.flatMap( x => x._2.map( y => (x._1, y.info, y.svar) ) )

  def execOnSVar[T, U]( info : ConvertibleTrait[T] )( f : SVar[T] => U ) : Option[U] =
    get(info).collect{ case svar => f.apply(svar) }

  /**
   *  injects a svar into this entity
   * @param sVar the svar to be injected
   * @param info a convertible trait representing the svar's type
   * @return an entity containing the given svar
   */
  def injectSVar[T](sVar : SVar[T], info : ConvertibleTrait[T]) : Entity =
    injectSVar(info.sVarIdentifier, sVar, info )

  /**
   *  creates and injects a svar using the given Provide instance
   * @param info the Provide instance specifying the svar to be created and injected
   * @return a tuple containing the created svar and an entity containing it
   */
  def injectSVar[T1, T2]( info : Provide[T1, T2] ) : (SVar[T1], Entity)=
    info.wrapped.injectSVar(this)

  /**
   *  returns a svar retrieved by using the svaridentifier from the convertible trait. converts it to match the
   * given convertible trait
   * @param out the convertible trait specifiying the name and type of the svar to be returned
   * @return a svar matching the given convertible trait's description
   */
  def get[T](out : ConvertibleTrait[T]) : Option[SVar[T]] =
    access(out.sVarIdentifier).headOption.collect{ case x => create(x, out, None) }

  def get[T](out : ConvertibleTrait[T], annotations : Seq[SVal[Semantics]]) : SVarList[T] =
    annotations.foldLeft(new SVarList(access(out))){ _.filter(_) }

  def getAll[T](out : ConvertibleTrait[T]) : List[SVar[T]] =
    access(out.sVarIdentifier).map( container => create(container, out, None) )

  /**
   *  returns a svar retrieved by using the svaridentifier from the convertible trait. converts it to match the
   * given convertible trait. Using the given reverter is enforced
   * @param out the convertible trait specifiying the name and type of the svar to be returned
   * @param reverter the reverter to be used
   * @return a svar matching the given convertible trait's description
   */
  def get[T](out : ConvertibleTrait[T], reverter : IReverter[T, _]) : Option[SVar[T]] =
    access(out.sVarIdentifier).headOption.collect{ case x => create(x, out, Some(reverter)) }

  def get[T](out : ConvertibleTrait[T], reverter : IReverter[T, _], annotations : Seq[SVal[Semantics]]) : SVarList[T] =
    new SVarList(access(out.sVarIdentifier).filter( _.annotations.forall( annotations.contains(_) ) ).map{
      container => SVarContainer(create(container, out, Some(reverter)), out, container.annotations )
    })

  def getAll[T](out : ConvertibleTrait[T], reverter : IReverter[T, _]) : List[SVar[T]] =
    access(out.sVarIdentifier).map( container => create(container, out, Some(reverter)) )

  //! returns nice string representation of this entity
  override def toString =
    super.toString + " with " + sVars.toString

  //! redirects the hashCode method call to the UUIDs hashCode method
  override def hashCode =
    id.hashCode

  //! redirects the equals method call to the UUIDs equals method
  override def equals(obj: Any) = obj match {
    case that : Entity => that.id == id
    case _ => false
  }

  /**
   * returns a SVarContainerBase containing the svar and information on its type
   * @param sVarName a symbol specifying the svar to be retrieved
   * @return a SVarContainerBase containing the svar and information on its type  
   */
  protected def access( sVarName : Symbol ) : List[SVarContainer[_]] =
    sVars.getOrElse(sVarName, Nil)

  protected def access[T]( out : ConvertibleTrait[T] ) : List[SVarContainer[T]] =
    sVars.getOrElse(out.sVarIdentifier, Nil).asInstanceOf[List[SVarContainer[T]]]

  /**
   *  injects a svar into this entity
   * @param sVarName the symbol used to inject the svar
   * @param sVar the svar to be injected
   * @param info a convertible trait representing the svar's type
   * @return an entity containing the given svar
   */
  private[entity] def injectSVar[T](sVarName : Symbol, sVar : SVar[T],
                                    info : ConvertibleTrait[T], annotations : Set[SVal[Semantics]] = Set()) : Entity =
    new Entity( sVars.updated(sVarName, (SVarContainer( sVar, info, annotations )) :: sVars.getOrElse(sVarName, Nil) ) , id )

  /**
   *  creates a wrapper around an svar (if necessary)
   * @param in the container containing the svar
   * @param out the convertible trait specifying the type of the return value
   * @param reverter an optionally used reverter (if not specified it will be looked up)
   * @return an svar of the required type  
   */
  private def create[T, U]( in : SVarContainer[U], out : ConvertibleTrait[T], reverter : Option[IReverter[T, _]] ) =
    if (in.info.typeinfo == out.typeinfo)
      in.svar.asInstanceOf[SVar[T]]
    else reverter match {
      case None => new ConvertedSVar(in.svar, in.info requiredAs out)
      case Some(r : IReverter[_, _]) if r.canRevert(out, in.info) =>
        new ConvertedSVar(in.svar, in.info requiredAs out using r.asInstanceOf[IReverter[T, U]])
      case Some(r) => throw new InvalidConverterException
    }
}

//! Some Exception which is thrown when the wrong converter was specified
class InvalidConverterException extends Exception
//! Some Exception which is thrown when the requested svar does not exist within an entity
class SVarDoesNotExistException( val name : Symbol ) extends Exception(name.toString())


/**
 * implementation of the SVarContainerBase trait
 * For internal use 
 */
protected case class SVarContainer[T]( val svar : SVar[T],
                                       val info : ConvertibleTrait[T],
                                       val annotations : Set[SVal[Semantics]] )
