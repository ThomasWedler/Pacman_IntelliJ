package siris.core.entity.description

import siris.core.component.Component
import siris.core.entity.typeconversion._

//! helper for the time EntityAspect is still used elsewhere
trait AspectBase{
  def toEntityAspect      : EntityAspect
  def getFeatures         : Set[ConvertibleTrait[_]]
  def getProvidings       : Set[ConvertibleTrait[_]]

  final def getRequirings : Set[ConvertibleTrait[_]] =
    getFeatures -- getProvidings
}

object EntityAspect{
  def apply(componentType : SVal[Semantics], createParamSet : NamedSValList,
            features : ProvideAndRequire*) : EntityAspect =
    EntityAspect(componentType, createParamSet, Seq(), features)
}

/**
 *  a data structure to hold the entity creation information for one specific component
 */
//TODO: this class has to be protected, but as it is used everywhere, this has to be done later
case class EntityAspect protected[description](componentType  : SVal[Semantics],
                                               createParamSet : NamedSValList,
                                               overrides      : Seq[ProvideAndRequire],
                                               features       : Seq[ProvideAndRequire]) extends AspectBase
{
  val targets : List[Symbol] = Nil

  def getFeatures = features.foldLeft(Set[ConvertibleTrait[_]]()){
    (set, elem) => elem match {
      case Provide(c) => set + c.from
      case Require(c) => set + c.from
    }
  }


  //TODO: this is quite ugly, but for now it does the trick. Replace it with an EntityAspect class that supports targets later
  def forTargets( trgts : List[Symbol] ) : EntityAspect = {
    val retVal = new EntityAspect(componentType, createParamSet, overrides, features){ override val targets = trgts }
    if (retVal.isInvalid)
      printError()
    retVal
  }

  def getProvidings = features.foldLeft(Set[ConvertibleTrait[_]]()){
    (set, elem) => elem match {
      case Provide(c) => set + c.from
      case _          => set
    }
  }

  /**
   * checks if this EntityAspect is invalid
   * @return true if the EntityAspect is invalid, false if it's valid
   */
  def isInvalid : Boolean = {
    var provides : List[Symbol] = Nil
    !features.forall {
      _ match {
        case Require(_) => true
        case Own(_)     => true
        case Provide(p) if ( provides.contains(p.getSVarName) ) => false
        case Provide(p) =>
          provides = p.getSVarName :: provides
          val tmp = targets.length == 1 || (targets.isEmpty && Component(componentType).length == 1)
          if (!tmp)
            println(componentType + " " + Component(componentType))
          false
          tmp
      }
    }
  }

  /**
   * creates an EntityAspect from this aspect (returns "this")
   * @return this
   */
  def toEntityAspect =
    this

  protected[entity] def +( f : Set[ConvertibleTrait[_]] ) : EntityAspect =
    EntityAspect(componentType, createParamSet, (features.toSet ++ f.map( _.isProvided) ).toSeq : _*)

  /**
   * prints an error message saying there are too many targets
   */
  private def printError() =
    sys.error("You must not provide a svar and have more than one target:\n" + toString + "\n\nPlease split EntityAspect")

  /**
   * a print bit more information on this aspect
   * @return a string containing information on this aspect
   */
  override def toString =
    "EntityAspect " + createParamSet.semantics + " for Components of type " + componentType.value.toSymbol.name +
      (if (targets.isEmpty) "" else " for targets: " + targets)
}
