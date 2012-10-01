package siris.core.entity.description

import siris.core.entity.typeconversion.ProvideAndRequire

/**
 * User: dwiebusch
 * Date: 12.04.11
 * Time: 08:53
 */

/**
 * Base for all CreateParameterSets which are Provided by Components (e.g. SpherePhysCreateParamSet)
 */
abstract class Aspect(val componentType : SVal[Semantics], val aspectType : SVal[Semantics], val targets : List[Symbol])
  extends AspectBase
{
  /**
   * creates an aspect from this createparamset (just a shortcut)
   * @return an aspect
   */
  def where( overrides : ProvideAndRequire*) : EntityAspect =
    EntityAspect(componentType, getCreateParams, overrides, providings ++ requirings ).forTargets(targets)

  /**
   * creates an EntityAspect from this Aspect
   * @return an EntityAspect
   */
  def toEntityAspect =
    EntityAspect(componentType, getCreateParams, Seq(), providings ++ requirings ).forTargets(targets)

  protected def getCreateParams : NamedSValList

  private def providings : Seq[ProvideAndRequire] =
    getProvidings.map(_.isProvided).toSeq

  private def requirings : Seq[ProvideAndRequire] =
    getRequirings.map(_.isRequired).toSeq

  /**
   * wrapper to make cvar adding easier
   */
  protected def addCVars( tuples : => TraversableOnce[SVal[_]] ) : NamedSValList =
    tuples.foldLeft(new NamedSValList(aspectType))( (cps, cvar) => cps += cvar )
}