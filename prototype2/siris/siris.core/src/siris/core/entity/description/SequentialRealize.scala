package siris.core.entity.description

import siris.core.entity.Entity
import siris.core.entity.component.Removability

/**
 * Created by IntelliJ IDEA.
 * User: stephan_rehfeld
 * Date: 21.07.11
 * Time: 13:35
 * To change this template use File | Settings | File Templates.
 */

class SequentialRealize private (
  private var handlerAllowed: Boolean,
  private var entityDescriptions: List[EntityDescriptionInterface[Entity]],
  private var resultCallbacks: List[Option[(Entity with Removability => Unit)]]) {

  def this() = this(false, List[EntityDescriptionInterface[Entity]](), List[Option[(Entity with Removability=> Unit)]]())

//  private var handlerAllowed = false
//  private var entityDescriptions = List[EntityDescription]()
//  private var resultCallbacks = List[Option[(Entity => Unit)]]()

  def whereResultIsProcessedBy( callback : Entity with Removability => Unit ) : SequentialRealize = {
    require( handlerAllowed, "You must pass an entity description first!" )
    handlerAllowed = false
    resultCallbacks = resultCallbacks.reverse.tail.reverse ::: Some(callback) :: Nil
    this
  }

  def thenRealize( entityDescription : EntityDescriptionInterface[Entity] ) : SequentialRealize = {
    entityDescriptions = entityDescriptions ::: entityDescription :: Nil
    resultCallbacks = resultCallbacks ::: None :: Nil
    handlerAllowed = true
    this
  }

  /**
   * Returns a new SequentialRealize that first realizes
   * this and than that.
   */
  def :::(that: SequentialRealize): SequentialRealize =
    new SequentialRealize(
      that.handlerAllowed,
      that.entityDescriptions ::: entityDescriptions,
      that.resultCallbacks ::: resultCallbacks)


  def execute = {
    var previous = (() => {})
    val entityDescriptionsReversed = entityDescriptions.reverse
    var resultCallbacksReversed = resultCallbacks.reverse
    for( entityDescription <- entityDescriptionsReversed ) {
      val processor = resultCallbacksReversed.head.getOrElse( ( e : Entity with Removability ) => {/*println( "No callback" )*/}  )
      resultCallbacksReversed = resultCallbacksReversed.tail
      val next = previous
      val handler = (entity : Entity  with Removability ) => {
        processor( entity )
        next()
      }

      previous = () => entityDescription.realize( handler )

    }
    previous()

  }

}

object SequentialRealize {
  def apply( entityDescription : EntityDescriptionInterface[Entity] ) : SequentialRealize = {
    new SequentialRealize().thenRealize(entityDescription)
  }
  def apply( entityDescriptions: TraversableOnce[EntityDescriptionInterface[Entity]] = List[EntityDescriptionInterface[Entity]]() ): SequentialRealize = {
    val seqRealize = new SequentialRealize()
    entityDescriptions.foreach(desc => seqRealize.thenRealize(desc))
    seqRealize
  }
}