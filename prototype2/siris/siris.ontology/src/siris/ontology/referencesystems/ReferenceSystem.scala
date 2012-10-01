package siris.ontology.referencesystems

import java.util.UUID

/**
 * User: dwiebusch
 * Date: 16.05.11
 * Time: 11:26
 */

trait ReferenceSystem[T]{
  private var mappings = Map[UUID, T]()
  private var inverses = Map[UUID, T]()

  protected def getMapping( id : UUID ) =
    mappings.get( id )

  protected def getInverse( id : UUID ) =
    inverses.get( id )

  protected def addMapping( id : UUID, value : T, inverse : T ) : UUID = {
    mappings = mappings.updated(id, value)
    inverses = inverses.updated(id, inverse)
    return id
  }

  protected def convertTo[U, V]( toConvert : U, inSystem : T, outSystem : T ) : V
}

case class NoMappingException(name : String) extends Exception("Could not find mapping for " + name)
case class NoConversionPossibleException(obj : Any)  extends Exception("Can not convert " + obj.toString)