//AutoGenerated Begin
//DO NOT EDIT!
package siris.components.editor.gui

import scala.swing._
import de.bht.jvr.core.Texture2D

class Texture2DSVarViewGenerator4a36f824523949c3b53fbf0575807bbd extends SVarViewGenerator[Texture2D] {

  def generate: SVarView[Texture2D] = new SVarView[Texture2D] {

//AutoGenerated END
//Put your code below

    /**
     *  The scala.swing.Component that visualizes the value.
     */
    //Todo: Implement yourself!
    //val component = new Label
    val content = new Label("content")
    val label = new Label ("South")
    val component = new BorderPanel {
      add(content, BorderPanel.Position.Center)
      add(label, BorderPanel.Position.South)
    }

    /**
      *  This function is called whenever the visualized value changes.
      *  It should update component accordingly.
      */
    //Todo: Implement yourself!
    def update(sVarValue: Texture2D) {
      //component.text = sVarValue.toString+" "+sVarValue.getWidth+"x"+sVarValue.getHeight

      //TODO: !!!!
      label =

      val icon = new javax.swing.ImageIcon(sVarValue.getImageData)
      //component.text = icon.getImage.hashCode().toString





    }

  }

  /**
   *  The name of this visualizer.
   *  This must not be unique.
   */
  //Todo: Name it!
  val name: String = "Texture2D View"

}