//AutoGenerated Begin
//DO NOT EDIT!
package siris.components.editor.gui

import scala.swing._
import simplex3d.math.floatm.Mat4f

class Mat4fSVarViewGeneratore1688f7d34ec439e8fe5685c6da36f9c extends SVarViewGenerator[Mat4f] {

  def generate: SVarView[Mat4f] = new SVarView[Mat4f] {

//AutoGenerated END
//Put your code below

    /**
     *  The scala.swing.Component that visualizes the value.
     */
    //Todo: Implement yourself!
    val component = new Label

    /**
      *  This function is called whenever the visualized value changes.
      *  It should update component accordingly.
      */
    //Todo: Implement yourself!
    def update(sVarValue: Mat4f) {
      component.text = sVarValue.m03.formatted("%4.3f%n") + ", "  +  sVarValue.m13.formatted("%4.3f%n") + ", "  + sVarValue.m23.formatted("%4.3f%n")
    }

  }

  /**
   *  The name of this visualizer.
   *  This must not be unique.
   */
  //Todo: Name it!
  val name: String = "Position View"

}