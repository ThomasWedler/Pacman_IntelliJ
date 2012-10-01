/*
  * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/24/11
 * Time: 1:49 PM
 */
package siris.components.editor.gui

import swing._

/**
 *
 *    Used to define an interface for a swing component that displays
 *          details of a siris.components.editor.gui.TreeNode
 */
abstract class DetailsView {

  /**
   *      The TreeNode this DetailsComponent shows details of.
   *
   *   Used by the EditorPane to determine if update has to be called.
   */
  val node: TreeNode

  /**
   *  Called by the editor panel, if node has changed.
   */
  def update(): Unit

  /**
   * The component that visualizes the siris.components.editor.gui.TreeNode
   */
  val component: Component
}

/**
 *  A scrollable container for DetailViews
 */
class DetailsScrollPane(private var dv: DetailsView) extends ScrollPane() {

  def detailsView_= (newDetailsView: DetailsView) = {
    dv = newDetailsView
    contents= newDetailsView.component
  }

  def detailsView = dv

  contents = dv.component

}