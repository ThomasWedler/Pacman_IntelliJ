/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/16/11
 * Time: 10:53 AM
 */
package siris.components.editor.gui

import swing.TreeModel
import swing.Tree._
import swing.Tree
import javax.swing.tree.TreePath
import swing.event.TreePathSelected
import javax.swing.event.{TreeSelectionListener, TreeModelEvent}
;

/**
 *
 *  A class that extdends TreeModel and gives access to customly emit structural change events.
 */
class AdvTreeModel[A](override val roots: Seq[A], children: A => Seq[A]) extends TreeModel[A](roots, children) {

  val hr = peer.getRoot

  private def getChildrenOf(parent: Any) = parent match {
    case `hr` => roots
    case a => children(a.asInstanceOf[A])
  }

  private def createEvent(path: TreePath, newValue: Any) = new TreeModelEvent(this, path,
    Array(getChildrenOf(path.getPath.last) indexOf newValue),
    Array(newValue.asInstanceOf[AnyRef]))

  /**
   * @see AdvTree.fireStructureChanged
   */
  def fireStructureChanged(path: Path[A], result: Any) =
    peer.treeModelListeners.foreach(_.treeStructureChanged(createEvent(pathToTreePath(path), result)))

  /**
   * @see AdvTree.fireNodesChanged
   */
  def fireNodesChanged(path: Path[A], result: Any) =
    peer.treeModelListeners.foreach(_.treeNodesChanged(createEvent(pathToTreePath(path), result)))

  /**
   * @see AdvTree.prepareNodeRemoval
   */
  def prepareNodeRemoval(path: Path[A], result: Any) =
    createEvent(pathToTreePath(path), result)

  /**
   * @see AdvTree.fireNodesRemoved
   */
  def fireNodesRemoved(event: TreeModelEvent) =
    peer.treeModelListeners.foreach(_.treeNodesRemoved(event))

  /**
   * @see AdvTree.fireNodesInserted
   */
  def fireNodesInserted(path: Path[A], result: Any) = {
    peer.treeModelListeners.foreach(_.treeNodesInserted(createEvent(pathToTreePath(path), result)))
  }
}

/**
 *
 *  A class that extdends Tree and gives access to customly emit structural change events.
 */
class AdvTree[A](private var treeDataModel: TreeModel[A] = TreeModel.empty[A]) extends Tree[A](treeDataModel) {
  private var myTreeData: AdvTreeModel[A] = null

  def treeData_=(tm: AdvTreeModel[A]) = {
    if (treeDataModel != null)
      treeDataModel.peer.removeTreeModelListener(modelListener)

    treeDataModel = tm
    peer.setModel(tm.peer)
    treeDataModel.peer.addTreeModelListener(modelListener)
    myTreeData = tm
  }

  private val me = this

  private var cp: Path[A] = List[A]()

  def currentPath: Path[A] = cp

  peer.getSelectionModel.addTreeSelectionListener(new TreeSelectionListener {
    def valueChanged(e: javax.swing.event.TreeSelectionEvent) {
      val (newPath, oldPath) = e.getPaths.map(treePathToPath).toList.partition(e.isAddedPath(_))
      me.publish(new TreePathSelected(me, newPath, oldPath, Option(e.getNewLeadSelectionPath: Path[A]), Option(e.getOldLeadSelectionPath: Path[A])))
      Option(e.getNewLeadSelectionPath: Path[A]).collect({case value => cp = value})
    }
  })

  /**
   *          Invoked after a node (or a set of siblings) has changed in some way.
   *       The node(s) have not changed locations in the tree or altered their children arrays,
   *                but other attributes have changed and may affect presentation.
   *                Example: the name of a file has changed, but it is in the same location in the file system.
   *
   *  @param path   A List of Objects identifying the path to the parent of the modified item,
   *                where the first element of the List is the Object stored at the root node and
   *                the last element is the Object stored at the parent node.
   *  @param result The inserted, removed or changed object. ("Not sure:" In case of a removal this can be null too)
   */
  def fireStructureChanged(path: Path[A], result: Any) = myTreeData.fireStructureChanged(path, result)

  /**
   *          Invoked after nodes have been inserted into the tree.
   *
   *  @param path   A List of Objects identifying the path to the parent of the modified item,
   *                where the first element of the List is the Object stored at the root node and
   *                the last element is the Object stored at the parent node.
   *  @param result The inserted, removed or changed object. ("Not sure:" In case of a removal this can be null too)
   */
  def fireNodesChanged(path: Path[A], result: Any) = myTreeData.fireNodesChanged(path, result)

  /**
   *                Invoked BEFORE nodes are removed from the tree.
   *                Remove the described nodes after this call from the tree data and then call fireNodesRemoved.
   *
   *                Note that if a subtree is removed from the tree,
   *                this method may only be invoked once for the root of the removed subtree,
   *                not once for each individual set of siblings removed.
   *
   *  @param path   A List of Objects identifying the path to the parent of the modified item,
   *                where the first element of the List is the Object stored at the root node and
   *                the last element is the Object stored at the parent node.
   *  @param result The removed object.
   *
   *  @return       A description of the upcomming removal that is need by fireNodesRemoved.
   *  @see          fireNodesRemoved
   */
  def prepareNodeRemoval(path: Path[A], result: Any) = myTreeData.prepareNodeRemoval(path, result)

  /**
   *                Invoked AFTER nodes have been removed from the tree.
   *                This should only be called after a invocation of prepareNodeRemoval and a
   *                subsequent removal of the described nodes from the tree data.
   *
   *                Note that if a subtree is removed from the tree,
   *                this method may only be invoked once for the root of the removed subtree,
   *                not once for each individual set of siblings removed.
   *  @param event  A description of the removal obtained by a invokation of prepareNodeRemoval.
   *  @see          prepareNodeRemoval
   */
   def fireNodesRemoved(event: TreeModelEvent) = myTreeData.fireNodesRemoved(event)

  /**
   *          Invoked after the tree has drastically changed structure from a given node down.
   *
   *  @param path   A List of Objects identifying the path to the parent of the modified item,
   *                where the first element of the List is the Object stored at the root node and
   *                the last element is the Object stored at the parent node.
   *  @param result The inserted, removed or changed object. ("Not sure:" In case of a removal this can be null too)
   */
  def fireNodesInserted(path: Path[A], result: Any) = myTreeData.fireNodesInserted(path, result)
}