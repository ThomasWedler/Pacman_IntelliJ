/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/26/11
 * Time: 5:07 PM
 */
package siris.components.editor.gui

import scala.swing._

class AdvComboBox[A](items: Seq[A]) extends ComboBox(items) {
  def getItems = (for(i <- 0 until peer.getModel.getSize) yield peer.getModel.getElementAt(i).asInstanceOf[A]).toSeq

  peer.addActionListener(Swing.ActionListener{
    e => publish(event.SelectionChanged(this))
  })

}
