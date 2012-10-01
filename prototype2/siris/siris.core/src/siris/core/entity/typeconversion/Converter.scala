package siris.core.entity.typeconversion

/**
 * @author dwiebusch
 * @date: 27.08.2010
 *
 * NOTE: Generally the type parameter O represents the type used within the Simulator core (O for Ontology). On the
 * other hand T stands for the component specific type.
 *
 */

/**
 *  base for converters and reverters. as stated above, tManifest is the manifest representing the components
 * type, whereas oManifest represents the type used within the Simulator core
 */
trait ConverterBase{
  protected def tManifest : ClassManifest[_]
  protected def oManifest : ClassManifest[_]
}

/**
 *  base class for user (e.g. component developer) defined converters. provides constructor to make creating
 * converters a lot easier. A Converter has to provide convert and revert methods
 */
abstract class Converter[T, O](implicit val tManifest : ClassManifest[T], implicit val oManifest : ClassManifest[O])
        extends IConverter[T, O] with IReverter[T, O]{
  /**
   * constructor for converters
   *
   * @param c1 the ConvertibleTrait representing the type used within the component
   * @param c2 the ConvertibleTrait representing the type used within the Simulator core
   */
  def this(c1 : ConvertibleTrait[T], c2 : ConvertibleTrait[O]) = this()(c1.typeinfo, c2.typeinfo)
}

/**
 *  The Converter object is used for process-wide registration of converters. Every Converter is registered here
 * to avoid the necessity of specifiying the converter to be used (unless one wants to do so)
 */
object Converter{
  //! the list of registered converters
  //TODO: using a list is bad for performance, fix this
  private var registeredConverters = List[ConverterBase]()

  /**
   *  register the provided converter
   * @param c the converter to be registered
   *
   */
  private[typeconversion] def register( c : ConverterBase) =
    synchronized( registeredConverters = c :: registeredConverters )

  /**
   * returns a matching converter
   * @param inputHint a ConvertibleTrait describing the input type
   * @param outputHint a ConvertibleTrait describing the output type
   * @return a converter which is capable of converting the input type into the output type
   */
  def apply[T, O](inputHint : ConvertibleTrait[T], outputHint : ConvertibleTrait[O],
                  list : List[ConverterBase] = NC :: registeredConverters) : IConverter[T, O] = list match {
    case Nil => throw NoConverterFoundException(inputHint, outputHint)
    case (converter : IConverter[_, _]) :: tail =>
      if (converter._canConvert(inputHint, outputHint))
        converter.asInstanceOf[IConverter[T, O]]
      else
        apply(inputHint, outputHint, tail)
  }
}

/**
 *  The Reverter object is used for process-wide registration of reverters. Every Reverter is registered here
 * to avoid the necessity of specifiying the reverter to be used (unless one wants to do so)
 */
object Reverter{
  //! the list of registered reverters
  //TODO: using a list is bad for performance, fix this
  private var registeredReverters = List[ConverterBase]()

  /**
   *  register the provided reverter
   * @param c the reverter to be registered
   */
  private[typeconversion] def register( c : ConverterBase) =
    synchronized( registeredReverters = c :: registeredReverters )

  /**
   * returns a matching reverter
   * @param outputHint a ConvertibleTrait describing the output type
   * @param inputHint a ConvertibleTrait describing the input type     *
   * @return a converter which is capable of converting the input type into the output type
   */
  def apply[T, O](outputHint : ConvertibleTrait[T], inputHint : ConvertibleTrait[O],
                  list : List[ConverterBase] = NC :: registeredReverters) : IReverter[T, O] = list match {
    case Nil => throw NoReverterFoundException(inputHint, outputHint)
    case (reverter : IReverter[_, _]) :: tail =>
      if (reverter._canRevert(outputHint, inputHint))
        reverter.asInstanceOf[IReverter[T, O]]
      else
        apply(outputHint, inputHint, tail)
  }
}

/**
 *  the internal base class of converters
 */
protected[entity] trait IConverter[-T, +O] extends ConverterBase{
  /**
   * this method may be overwritten to add additional checking when a converter is tested to match the conversion task
   * for internal use only
   * @param from a description of the input type
   * @param to a description of the output type
   * @return true if the converter can convert from from to to ;-) false if it can't
   */
  protected[entity] def canConvert(from : ConvertibleTrait[_], to : ConvertibleTrait[_]) : Boolean  = true

  /**
   * the actual conversion function
   * @param i the input data to be converted
   * @return the converted data
   */
  def convert( i : T ) : O

  /**
   * internal check for convertibility, based on type information
   *
   * calls the canConvert method
   *
   * @param from information on the input type
   * @param to information on the output type
   * @return true if the converter can convert from from to to ;-) false if it can't
   */
  private[typeconversion] def _canConvert(from : ConvertibleTrait[_], to : ConvertibleTrait[_]) : Boolean =
    if (from.typeinfo <:< tManifest && oManifest <:< to.typeinfo) canConvert(from, to) else false

  //! register this converter
  Converter.register(this)
}

/**
 *  the internal base class of reverters
 */
protected[entity] trait IReverter[+T, -O] extends ConverterBase{
  /**
   * this method may be overwritten to add additional checking when a reverter is tested to match the reversion task
   * !!! for internal use only !!!
   * 
   * @param to a description of the output type
   * @param from a description of the input type
   * @return true if the reverter can revert from from to to ;-) false if it can't
   */
  protected[entity] def canRevert(to : ConvertibleTrait[_], from : ConvertibleTrait[_]) : Boolean = true

  /**
   * the actual reversion function
   * @param i the input data to be reverted
   * @return the reverted data
   */
  def revert( i : O ) : T

  /**
   * internal check for revertibility, based on type information
   *
   * calls the canRevert method
   * 
   * @param to information on the output type
   * @param from information on the input type
   * @return true if the reverter can revert from from to to ;-) false if it can't
   */
  private[typeconversion] def _canRevert(to : ConvertibleTrait[_], from : ConvertibleTrait[_]) : Boolean =
    if (from.typeinfo <:< oManifest && tManifest <:< to.typeinfo ) canRevert(to, from) else false

  //! register this reverter
  Reverter.register(this)
}


/**
 * the null converter (does not con- or revert anything but provides input- as output-data)
 */
private object NC extends Converter[Any, Any]{
  override private[typeconversion] def _canConvert(from: ConvertibleTrait[_], to: ConvertibleTrait[_]) = canConvert(from, to)
  override private[typeconversion] def _canRevert(to: ConvertibleTrait[_], from: ConvertibleTrait[_])  = canRevert(from, to)
  override def canConvert(from: ConvertibleTrait[_], to: ConvertibleTrait[_]) = to.typeinfo >:> from.typeinfo
  override def canRevert(to: ConvertibleTrait[_], from: ConvertibleTrait[_])  = to.typeinfo >:> from.typeinfo
  def convert(i: Any) = i
  def revert(i: Any)  = i
}

// Some Exception definitions
case class NoConverterFoundException(from : ConvertibleTrait[_], to : ConvertibleTrait[_]) extends Exception(
  from.typeinfo + " => " + to.typeinfo
  )
case class NoReverterFoundException(from : ConvertibleTrait[_], to : ConvertibleTrait[_]) extends Exception(
  from.typeinfo + " => " + to.typeinfo
  )