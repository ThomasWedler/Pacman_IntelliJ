/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/25/11
 * Time: 10:01 AM
 */
package siris.components.editor.gui

import swing.Component

/**
 *  Holds information about SVarViewException exceptions
 */
case class SVarViewException(errorMessage: String) extends java.lang.Throwable {
  override def toString: String = errorMessage
}

abstract class SVarViewBase {
  val component: Component
  def internalUpdate(sVarValue: Any)
}

abstract class SVarView[T] extends SVarViewBase {
  type sVarValueType  = T

  final def internalUpdate(sVarValue: Any) {
    try {
      update(sVarValue.asInstanceOf[sVarValueType])
    }
    catch {
      case _ => throw SVarViewException("SVarView can not handle a value of type " + sVarValue.asInstanceOf[AnyRef].getClass.getCanonicalName)
    }
  }

  def update(sVarValue: sVarValueType)
}

abstract class SVarViewGeneratorBase {
  val name: String
  def generate: SVarViewBase
  override def toString(): String = name
}

abstract class SVarViewGenerator[T] extends SVarViewGeneratorBase{
  def generate: SVarView[T]
}