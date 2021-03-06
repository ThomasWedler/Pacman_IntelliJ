//AutoGenerated Begin
//DO NOT EDIT!
package siris.components.editor.gui

import scala.swing._
import simplex3d.math.floatm.Mat4f

class Mat4fSVarViewGenerator171103effeec48f7ad46a05b01bc050e extends SVarViewGenerator[Mat4f] {

  def generate: SVarView[Mat4f] = new SVarView[Mat4f] {

//AutoGenerated END
//Put your code below

    val l0 = new Label
    val l1 = new Label
    val l2 = new Label
    val l3 = new Label

    /**
     *  The scala.swing.Component that visualizes the value.
     */
    //Todo: Implement yourself!
    val component = new GridPanel(4, 1) {
      contents += l0
      contents += l1
      contents += l2
      contents += l3
    }

    /**
      *  This function is called whenever the visualized value changes.
      *  It should update component accordingly.
      */
    //Todo: Implement yourself!
    def update(sVarValue: Mat4f) {
      l0.text = sVarValue.m00.formatted("%4.2f%n") + ", " + sVarValue.m01.formatted("%4.2f%n") + ", " + sVarValue.m02.formatted("%4.2f%n") + ", " + sVarValue.m03.formatted("%4.2f%n")
      l1.text = sVarValue.m10.formatted("%4.2f%n") + ", " + sVarValue.m11.formatted("%4.2f%n") + ", " + sVarValue.m12.formatted("%4.2f%n") + ", " + sVarValue.m13.formatted("%4.2f%n")
      l2.text = sVarValue.m20.formatted("%4.2f%n") + ", " + sVarValue.m21.formatted("%4.2f%n") + ", " + sVarValue.m22.formatted("%4.2f%n") + ", " + sVarValue.m23.formatted("%4.2f%n")
      l3.text = sVarValue.m30.formatted("%4.2f%n") + ", " + sVarValue.m31.formatted("%4.2f%n") + ", " + sVarValue.m32.formatted("%4.2f%n") + ", " + sVarValue.m33.formatted("%4.2f%n")
    }

  }

  /**
   *  The name of this visualizer.
   *  This must not be unique.
   */
  //Todo: Name it!
  val name: String = "Matrix View"

}