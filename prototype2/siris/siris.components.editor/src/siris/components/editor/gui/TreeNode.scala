/*
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 2/16/11
 * Time: 1:44 PM
 */
package siris.components.editor.gui

import siris.core.entity.Entity

import java.util.UUID
import siris.core.svaractor.SVar
import siris.core.entity.description.{NamedSValList, SVal}

abstract class TreeNode() {
  def children: Seq[TreeNode]
  def children_=(value: TreeNode*)
  def parent: Option[TreeNode]

  parent.collect({case p => p.children = (p.children :+ this):_*})

  def getPath: List[TreeNode] = {
    parent.map(_.getPath ::: (this :: Nil)).getOrElse(this :: Nil)
  }

  def getPathToParent: List[TreeNode] = {
    getPath.dropRight(1)
  }

  private val id = UUID.randomUUID

  /**
   *    Implemented to prevent an infinite loop when invoking java.lang.hashcode.
   * Somehow the scala println manages to invoke java.lang.Object.hashcode.
   *          Even though hashcode is overwritten here. //TODO investigate
   * @see     hascode
   */
  override def toString : String = {
    getClass.getName + "@" + Integer.toHexString(hashCode)
  }

  /**
   *    Implemented to prevent an infinite loop when invoking java.lang.hashcode.
   * The normal hashcode method would use the hashcodes of all members to calcualte
   *          the hashcode of this object. Since it is a double linked tree this would
   *          cause an infinite loop.
   */
  /**
   *  redirects the hashCode method call to the UUIDs hashCode method
   */
  override def hashCode = {
    id.hashCode
  }

  /**
   * @see     hashcode
   */
  /**
   *  redirects the equals method call to the UUIDs equals method
   */
  override def equals(obj: Any) = {
    obj match {
      case that: TreeNode => that.id == id
      case _ => false
    }
  }

}

abstract class TreeNodeWithValue extends TreeNode {
  def value: Any
}

case class EnRoot private(var appName: String, parent: Option[TreeNode], var children: TreeNode*) extends TreeNode {
  def this(appName: String, children: TreeNode*) = this(appName, None, children:_*)
}
case class EnEntity(e: Entity, parent: Option[TreeNode], var children: TreeNode*) extends TreeNode {
  var name: String = e.id.toString
}


case class EnSVarCollection(parent: Option[TreeNode], var children: TreeNode*) extends TreeNode
case class EnSVar(svar: SVar[_], name: Symbol, parent: Option[TreeNode], var children: TreeNode*) extends TreeNode
case class EnSVarValue(var value: Any, parent: Option[TreeNode], var children: TreeNode*) extends TreeNodeWithValue {
  def this(parent: Option[TreeNode], children: TreeNode*) = this("Not initialized", parent, children:_*)
}

case class EnCreateParamSet(component: Symbol, cps: NamedSValList, parent: Option[TreeNode], var children: TreeNode*) extends TreeNode
case class EnCreateParam(cpb: SVal[_], parent: Option[TreeNode], var children: TreeNode*) extends TreeNode
case class EnCreateParamValue(value: Any, parent: Option[TreeNode], var children: TreeNode*) extends TreeNodeWithValue {
  override def toString : String = {
    value.toString
  }
}
