/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 3/12/11
 * Time: 3:31 PM
 */
package siris.components.editor.gui

import swing.Component
import siris.core.svaractor.SVar

/**
 *  Holds information about SVarViewException exceptions
 */
case class SVarSetterException(errorMessage: String) extends java.lang.Throwable {
  override def toString: String = errorMessage
}

abstract class SVarSetterBase {
  val component: Component
  //Changes the setter to write to this svar.
  //If the underlying type of the svar is compatible to sVar is not checked.
  //If the types are not compatible, an exception is thrown.
  def changeRegisteredSvarTo(sVar: SVar[_]): Unit
  def internalUpdate(sVarValue: Any)
}

abstract class SVarSetter[T] extends SVarSetterBase {

  private var setter: Option[SVar[T]] = None

  final def changeRegisteredSvarTo(sVar: SVar[_]): Unit = {
    if(sVar != null)
      try {
        setter = Some(sVar.asInstanceOf[SVar[T]])
      } catch {
        case _ => throw SVarSetterException("SVarSetter can not handle a sVar of type " + sVar.containedValueManifest.erasure.getCanonicalName)
      }
  }

  final def internalUpdate(sVarValue: Any) {
    try {
      update(sVarValue.asInstanceOf[T])
    }
    catch {
      case _ => throw SVarViewException("SVarView can not handle a value of type " + sVarValue.asInstanceOf[AnyRef].getClass.getCanonicalName)
    }
  }

  def update(sVarValue: T): Unit = {}

  final def setSvar(newValue: T): Unit  = {
    setter.collect{case sVar => sVar.set(newValue)}
  }

  final def getSvar(handler: (T) => Unit) = {
    setter.collect{case sVar => sVar.get(handler)}
  }
}

abstract class SVarSetterGeneratorBase {
  val name: String
  def generate: SVarSetterBase
  override def toString(): String = name
}

abstract class SVarSetterGenerator[T] extends SVarSetterGeneratorBase{
  def generate: SVarSetter[T]
}