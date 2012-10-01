/*
  * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package siris.core.helper

import scala.collection._
import siris.core.svaractor.SVar
import scala.Some
import siris.core.entity.Entity
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.entity.description.{SVal, SValList}
import actors.Actor

/**
 *  A trait for updating component-internal entities and their associated svars using a hashmap.
 */
trait SVarUpdateFunctionMap {

  /**
   *  Stores a consume function for a svar.
   */
  protected class ChangeableConsume[T](var func: Option[T => Unit])

  /**
   *  Stores functions to update an SVar from the value of its associated internal entity and vice versa.
   * @see addSVarUpdateFunctions
   */
  protected case class GetAndSet[T](consumeSVar:  ChangeableConsume[T], var updateSVar: Option[() => Unit])

  private val updateFunctionMap = mutable.Map[SVar[_], GetAndSet[_]]()
  private val pausedFunctionMap = mutable.Map[SVar[_], GetAndSet[_]]()

  /**
   *    Value changes by SVarActors contained in this set do not trigger consume functions via observe.
   *  @see    siris.core.svaractor.SVar.observe()
   */
  var ignoredWriters: immutable.Set[Actor] = immutable.Set()

  /**
   *    Requests the values of svars that belong to one entity and applies a handler when every value has arrived.
   *  Initially observe and get is called on all desired svars using the mechanisms of
   *          the SVarUpdateFunctionMap trait. Whenever a (new) svar value arrives at the calling actor,
   *          it is stored locally. If at least one value has arrived for every svar, the given handler is called.
   *          This handler can then use all requested values at once.
   *
   * @param e               The entity the contains the desired svars
   * @param toGet           Identifiers for all desired svars
   * @param handler         A function that processes the requested svar values.
   *                        It is called once all values have arrived.
   * @param keepRegistered  If true the svars are not ignored (SVar mechanism) and are not removed from the
   *                        SVarUpdateFunctionMap trait's machanisms. That way the observe's consume functions can be
   *                        changed without calling ignore. updateConsumeSVar or removeSVarUpdateFunctions can be used
   *                        to change consume functions or ignore svars. If none of the previous two methods is called
   *                        and keepRegistered was true the svars stay observed using a consume function that does nothing.
   */
  def collectSVars(e: Entity, toGet: ConvertibleTrait[_]*)
                  (handler: SValList => Unit, keepRegistered: Boolean = false): Unit = {

    var retrievedValues = new SValList()

    toGet.foreach((ct: ConvertibleTrait[_]) => {init(ct.asInstanceOf[ConvertibleTrait[ct.dataType]])})

    def init[T](ct: ConvertibleTrait[T]) =
      addSVarUpdateFunctions(e.get(ct).get, Some(valueArrived[T](ct) _), None, true)

    def clean[T](ct: ConvertibleTrait[T]) = {
      updateConsumeSVar(e.get(ct).get, None)
      updateGetValueForSVar(e.get(ct).get, None)
    }

    def remove[T](ct: ConvertibleTrait[T]) =
      removeSVarUpdateFunctions(e.get(ct).get)

    def valueArrived[T](ct: ConvertibleTrait[T])(value: T): Unit = {
      retrievedValues = (retrievedValues - ct)
      retrievedValues += SVal(ct)(value)
      if (retrievedValues.size == toGet.size) done
    }

    def done: Unit = {
      if(keepRegistered)
        toGet.foreach((ct: ConvertibleTrait[_]) => {clean(ct.asInstanceOf[ConvertibleTrait[ct.dataType]])})
      else
        toGet.foreach((ct: ConvertibleTrait[_]) => {remove(ct.asInstanceOf[ConvertibleTrait[ct.dataType]])})
      handler(retrievedValues)
    }
  }

  /**
   *    Adds two update functions to update an SVar from the value of its associated internal entity and vice versa.
   *          If there where already functions registered for this svar, the method will return and print a warning.
   * Passing None is equal to passing functions that do nothing.
   * @param svar            An SVar
   * @param consumeSVar     A function, that updates the internal entity associated with svar, using the svar observe mechanism.
   * @param getValueForSVar A function that retrieves the corresponding value from the internal entity associated with svar.
   * @param useGet          If true, additionally to observe, get is called once on svar using consumeSVar.
   *                        This way the svars current value guaranteed to be retreeved at least once immediatelly,
   *                        even if it does not change immediatelly.
   * @see removeSVarUpdateFunctions
   */
  def addSVarUpdateFunctions[T](svar: SVar[T], consumeSVar: Option[T => Unit],
                                getValueForSVar: Option[() => T] = None, useGet: Boolean = false) : Unit = {
    if(updateFunctionMap.contains(svar)) {
      println("[warn][SVarUpdateFunctionMap] You tried to call addSVarUpdateFunctions passing a svar that has already been registered before.")
      return
    }
    val changeableConsume = new ChangeableConsume(consumeSVar)

    svar.observe(ignoredWriters)((newValue: T) => {changeableConsume.func.collect{case f => f(newValue)}})
    if(useGet) svar.get((newValue: T) => {changeableConsume.func.collect{case f => f(newValue)}})

    val updateSVar = getValueForSVar collect { case func => (() => svar.set(func.apply)) }
    updateFunctionMap += svar -> GetAndSet (changeableConsume, updateSVar)
  }

  /**
   *  Adds two update functions to update an SVar from the value of its associated internal entity and vice versa.
   *        If there where already functions registered for this svar, the method will return and print a warning.
   * @param svar            An SVar
   * @param consumeSVar     A function, that updates the internal entity associated with svar.
   * @param getValueForSVar A function that retrieves the corresponding value from the internal entity associated with svar.
   * @see removeSVarUpdateFunctions
   */
  def addSVarUpdateFunctions[T](svar: SVar[T], consumeSVar: T => Unit, getValueForSVar: => T) : Unit =
    addSVarUpdateFunctions(svar, Some(consumeSVar), Some(() => getValueForSVar))

  /**
   *  Removes the update functions for this svar.
   *        After this it is possible to call addSVarUpdateFunctions for this svar anew.
   */
  def removeSVarUpdateFunctions(svar: SVar[_]): Unit = {
    svar.ignore
    updateFunctionMap -= svar
  }

  /**
   *  Calles all updatedSvar functions that where previously added using addSVarUpdateFunctions.
   * @see addSVarUpdateFunctions
   */
  def updateAllSVars =
    updateFunctionMap.foreach{ _._2.updateSVar.collect{case func => func.apply}  }

  /**
   *    Pauses the update mechanism for this svar.
   * This saves the registered update functions, removes them from the updateFunctionMap and
   *          calls sVar.ignore.
   *          The update mechanism cam be resumed later on.
   */
  def pauseUpdatesFor(svar: SVar[_]): Unit =
    updateFunctionMap.get(svar).collect{
      case gAs =>
        updateFunctionMap -= svar
        pausedFunctionMap += svar -> gAs
        svar.ignore
    }


  /**
   *    Resumes the update mechanism for this svar.
   * This uses the stored update functions to resume updating
   *          just like before pauseUpdatesFor was called.
   * @param useGet If true, additionally to observe, get is called once on svar using consumeSVar.
   *               This way the svars current value guaranteed to be retreeved at least once immediatelly,
   *               even if it does not change immediatelly.
   * @see addSVarUpdateFunctions
   */
  def resumeUpdatesFor[T](svar: SVar[T], useGet: Boolean = false): Unit =
    pausedFunctionMap.get(svar).collect{
      case gAs =>
        pausedFunctionMap -= svar
        updateFunctionMap += svar -> gAs
        gAs.asInstanceOf[GetAndSet[T]].consumeSVar.func.collect{case func =>
          svar.observe(ignoredWriters)(func)
          if(useGet) svar.get(func)
        }
    }


  /**
   *  Changes the function that updates the internal rep when the svar has changed
   */
  private def updateConsumeSVar[T](svar: SVar[T], newConsumeSVar: Option[T => Unit]): Unit  =
    updateFunctionMap.get(svar).collect{
      case gAs => gAs.asInstanceOf[GetAndSet[T]].consumeSVar.func = newConsumeSVar
    }

  /**
   *  Changes the function that updates the internal rep when the svar has changed
   */
  def updateConsumeSVar[T](svar: SVar[T], newConsumeSVar: T => Unit): Unit   =
    updateConsumeSVar(svar, Some(newConsumeSVar))

  /**
   *  Changes the function that updates the internal rep when the svar has changed
   *        to do nothing
   */
  def disableConsumeSvar[T](svar: SVar[T]): Unit   =
    updateConsumeSVar(svar, None)

  /**
   *  Changes the function that updates the svar from the internal rep
   */
  private def updateGetValueForSVar[T](svar: SVar[T], newGetValueForSVar: Option[() => T]): Unit  =
    updateFunctionMap.get(svar).collect {
      case gAs => gAs.asInstanceOf[GetAndSet[T]].updateSVar = newGetValueForSVar match {
        case Some(func) => Some(() => {svar.set(func.apply)})
        case None => None
      }
    }


  /**
   *  Changes the function that updates the svar from the internal rep
   */
  def updateGetValueForSVar[T](svar: SVar[T], newGetValueForSVar: () => T): Unit  =
    updateGetValueForSVar(svar, Some(newGetValueForSVar))

  /**
   *  Disables the function that updates the svar from the internal rep
   */
  def disableGetValueForSVar[T](svar: SVar[T]): Unit  =
    updateGetValueForSVar(svar, None)

  /**
   *  Changes the function that updates the internal rep when the svar has changed and
   *        the function that updates the svar from the internal rep at once.
   * @see   updateConsumeSVar, updateGetValueForSVar
   */
  def updateSVarUpdateFunctions[T](svar: SVar[T], newConsumeSVar: T => Unit, newGetValueForSVar: () => T) = {
    updateConsumeSVar(svar, newConsumeSVar)
    updateGetValueForSVar(svar, newGetValueForSVar)
  }

}
