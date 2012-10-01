package siris.pacman.graph

import java.util.UUID
import simplex3d.math.floatm.Vec2f
import simplex3d.math.floatm.renamed._
import simplex3d.math.floatm.FloatMath._
import siris.java.JavaInterface
import collection.immutable.HashMap
import simplex3d.math.intm.Vec2i

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 1:15 PM
 */

/**
 * Utilities for graphs.
 */
object Graph {

  /**
   * Creates a graphical representation for arbitrary graphs.
   * @param root A node of the graph.
   * @param inst A JavaInterface instance (used to draw).
   */
  def drawGraph(root: Node, inst: JavaInterface) {

    val closed = new collection.mutable.HashSet[UUID]()
    case class ExpandTask(center: Vec2f, normal: Vec2f, radius: Float, scale: Float, node: Node)

    def drawNode(node: Node, center: Vec2f = Vec2.Zero, scale: Float = 1f) {
      val id = inst.loadObject(
        "pacman/models/blue-1x0.05x0.05-positiveZAxis-cylinder.dae",
        center.x, center.y, 0f, 10f*scale, 10f*scale, 0.05f,
        node.id,
        Some("pacman/models/green-1x0.05x0.05-positiveZAxis-cylinder.dae"))

      node.neighbors.foreach(nb => {inst.addStaticLine(nb.id, id)})
      closed += node.id
    }

    drawNode(root)
    var fringe = List(ExpandTask(Vec2f.Zero, Vec2f.UnitY, 3.0f, 1f, root))

    while(!fringe.isEmpty) {
      val task = fringe.head
      fringe = fringe.tail

      val nodes = task.node.neighbors.filter(nb => !closed.contains(nb.id))
      val start = Mat2x2(Mat2x3.rotate(radians(-90f))) * normalize(task.normal)
      val steps = nodes.size-1
      val angleStep = if(steps>0) 180.0f / steps.toFloat else 90f
      var i = 0

      nodes.foreach(node => {
        val newNormal = Mat2x2(Mat2x3.rotate(radians(angleStep*i.toFloat))) * start
        val newCenter = (newNormal * task.radius) + task.center
        val newScale = task.scale * 0.5f
        val newRadius = task.radius * 0.33f
        drawNode(node, newCenter, newScale)
        fringe = fringe ::: List(ExpandTask(newCenter, newNormal, newRadius, newScale, node))
        i += 1
      })
    }
  }
}

/**
 * Utilities for tile graphs.
 */
object TileGraph {

  /**
   * Checks if a node satisfies the constraints of a pacman-level-tile.
   * @param n The node to check.
   */
  def checkSanity(n: TileNode) {
    val exits = new collection.mutable.HashMap[Vec2f, Int] {override def default(key: Vec2f) = 0}
    n.neighbors.toList.foreach(_ match {
      case tn: TileNode => exits(toDir(tn.position.toVec - n.position.toVec)) += 1
      case _ =>
    })
    if(exits(Vec2i.Zero) != 0)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to a tile with the same position.")

    if(exits(Vec2i.UnitX) > 1)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to more than one eastern tiles.")
    if(exits(-Vec2i.UnitX) > 1)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to more than one western tiles.")
    if(exits(Vec2i.UnitY) > 1)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to more than one northern tiles.")
    if(exits(-Vec2i.UnitY) > 1)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to more than one southern tiles.")

    if(exits(Vec2i.UnitX+Vec2i.UnitY) > 0)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to at least one north-eastern tile.")
    if(exits(Vec2i.UnitX-Vec2i.UnitY) > 0)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to at least one south-eastern tile.")
    if(exits(-Vec2i.UnitX+Vec2i.UnitY) > 0)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to at least one north-western tile.")
    if(exits(-Vec2i.UnitX-Vec2i.UnitY) > 0)
      throw new Exception("Tile at position " + n.position.toVec + " is connected to at least one south-western tile.")
  }

  //Normal Vec2i to "pacman-level-directions" (left, right, up, ...)
  private def toDir(v: Vec2i) =
    Vec2i(
      if(v.x > 0)1 else if (v.x < 0)-1 else 0,
      if(v.y > 0)1 else if (v.y < 0)-1 else 0
    )
}