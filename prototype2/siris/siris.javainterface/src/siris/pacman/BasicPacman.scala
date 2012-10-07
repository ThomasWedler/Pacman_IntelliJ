package siris.pacman

import graph._
import siris.components.renderer.jvr.{JVRRenderWindowClosed, JVRConnector, SetAmbientColor}
import actors.Actor
import siris.components.worldinterface.WorldInterface
import siris.core.component.Component
import javax.swing.JOptionPane

//import impl.Test

import siris.core.svaractor.{SVarActorImpl, SVarActorLW}
import siris.core.helper.TimeMeasurement
import siris.java.JavaInterface
import simplex3d.math.intm.Vec2i
import simplex3d.math.floatm.FloatMath._
import java.util.UUID
import simplex3d.math.floatm.{Vec3f, Vec2f}
import java.awt.event.{KeyEvent, KeyListener}
import java.awt.Color

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 1:03 PM
 */

object BasicPacman {

  private var inst: Option[JavaInterface] = None
  var _inst: JavaInterface = null

  /**
   * Returns the JavaInterface used by the BasicPacman singleton.
   * BasicPacman.startPacman has to be called before.
   */
  def getJavaInterface = inst.getOrElse(
    throw new java.lang.Exception(
      "BasicPacman needs to be started before the JavaInterface can be obtained."))

  //----------------------------------------
  def setColorToRed {
    _inst.appActor.jvr ! SetAmbientColor(Actor.self, new de.bht.jvr.util.Color(0.8f, 0.4f, 0.4f))
  }

  def setColorToBlue {
    _inst.appActor.jvr ! SetAmbientColor(Actor.self, new de.bht.jvr.util.Color(0.4f, 0.4f, 0.8f))
  }

  def setColorToNormal {
    _inst.appActor.jvr ! SetAmbientColor(Actor.self, new de.bht.jvr.util.Color(0.8f, 0.8f, 0.8f))
  }

  def rotateEntityTo(id : UUID, angle : java.lang.Float) {
    _inst.rotateObjectTo(id, angle)
  }

  def moveEntity(id : UUID, x : java.lang.Float, y : java.lang.Float, z : java.lang.Float) {
    _inst.moveObjectTo(id, x, y, z)
  }

  def close {
    _inst.appActor ! JVRRenderWindowClosed(_inst.appActor)
  }

  /**
   * Sets up and starts a basic pacman application
   * @param ai An object controlling some ai operations
   * @param ambientLightingOnly Use ambient lighting only. (This is a hotfix for MacBookAirs)
   */
  def startPacman(ai: PacmanAI, levelRoot: Node, breathFirstSearch: GraphSearch, ambientLightingOnly: Boolean = false) {

    //Set up and start an application actor
    new SVarActorLW with TimeMeasurement {
      self =>

      private var pacman: Option[Pacman] = None
      private val movingEntityRadius = 0.2f

      private var ghosts = List[Ghost]()
      private var goodies = List[Goodie]()
      private val goodieRadius = 0.1f
      private var powerUps = List[PowerUp]()
      private val powerUpRadius = 0.15f

      _inst = if (ambientLightingOnly) new JavaInterface(new Color(0.8f, 0.8f, 0.8f), true)
      else new JavaInterface(false, false)

      inst = Some(_inst)

      val powerUpModel = if (ambientLightingOnly) "pacman/models/powerup-ambient-hotfix.dae" else "pacman/models/powerup.dae"
      val foodModel = if (ambientLightingOnly) "pacman/models/food-ambient-hotfix.dae" else "pacman/models/food.dae"

      //This method is called once after the actor is started
      override def startUp() {
        //_inst.startRenderer(800, 600)
        _inst.startRenderer(1440, 810)
        createGraphics(levelRoot, _inst)
        registerKeyhandler()
        Thread.sleep(1000);
        JOptionPane.showMessageDialog(null, "Press OK when rendering Game is completed.", "Start", JOptionPane.INFORMATION_MESSAGE);
        self ! WakeUpMessage
      }

      //Register for notification on certain keypresses
      private def registerKeyhandler() {
        _inst.addKeyListener(new KeyListener {
          def keyPressed(e: KeyEvent) {
            self ! e
          }

          def keyTyped(e: KeyEvent) {}

          def keyReleased(e: KeyEvent) {}
        })
      }

      var started = false

      //Handle keyevents
      addHandler[KeyEvent] {
        case e =>
          if (e.getKeyCode == KeyEvent.VK_UP) pacman.collect {
            case p => p.setDesiredMovementDirection(0, 1)
          }
          if (e.getKeyCode == KeyEvent.VK_DOWN) pacman.collect {
            case p => p.setDesiredMovementDirection(0, -1)
          }
          if (e.getKeyCode == KeyEvent.VK_LEFT) pacman.collect {
            case p => p.setDesiredMovementDirection(-1, 0)
          }
          if (e.getKeyCode == KeyEvent.VK_RIGHT) pacman.collect {
            case p => p.setDesiredMovementDirection(1, 0)
          }
          /*if (e.getKeyCode == KeyEvent.VK_SPACE) if (!started) {
            self ! WakeUpMessage;
            started = true
          }*/
      }

      //The application actors "main loop"
      addHandler[WakeUp] {
        case msg =>
          val deltaT = getDeltaT.toSeconds
          //println("Simulating with a deltaT of " + deltaT + " sec.")
          startTimeMeasurement()
          ai.onSimulationStep(deltaT)
          simulate(deltaT)
          checkForCollisions()
          requestWakeUpCall(timeToNextFrame())
      }

      private def simulate(deltaT: Float) {
        pacman.collect {
          case p => simulateMovingEntity(p, deltaT)
        }
        ghosts.foreach(simulateMovingEntity(_, deltaT))
      }

      private def checkForCollisions() {
        val movingEntities = pacman.map(p => List(p)).getOrElse(Nil) ::: ghosts
        val staticEntities = powerUps ::: goodies

        //Collisions with objects
        movingEntities.foreach(me => {
          staticEntities.foreach(se => {
            val distance = length(Vec2f(me.getPositionX, me.getPositionY) - Vec2f(se.getPositionX, se.getPositionY))
            val radii = (if (se.isInstanceOf[Goodie]) goodieRadius else powerUpRadius) + movingEntityRadius
            //Collision
            if (distance < radii) {
              ai.onCollision(me, se)
              ai.onCollision(se, me)
              if (me.isInstanceOf[Pacman]) {
                se.disconnect()
                //TODO: implement remove
                _inst.moveObjectTo(se.id(), -1000, -1000, -1000)
                se match {
                  case g: Goodie => goodies = goodies.filter(_ != g)
                  case p: PowerUp => powerUps = powerUps.filter(_ != p)
                }
              }
            }
          })
        })

        //Collisions between pacman and ghosts
        pacman.collect {
          case p =>
            ghosts.foreach(g => {
              val distance = length(Vec2f(p.getPositionX, p.getPositionY) - Vec2f(g.getPositionX, g.getPositionY))
              val radii = 2f * movingEntityRadius
              if (distance < radii) {
                ai.onCollision(p, g)
                ai.onCollision(g, p)
              }
            })
        }

      }

      //Move MovingEntityNodes according to the level and the desired direction
      private def simulateMovingEntity(e: MovingEntityNode, deltaT: Float) {
        var position = Vec2f(e.getPositionX, e.getPositionY)
        var desiredDir = toDir(Vec2i(e.getDesiredMovementDirectionX, e.getDesiredMovementDirectionY))
        var currentDir = toDir(Vec2i(e.getCurrentMovementDirectionX, e.getCurrentMovementDirectionY))
        var decisionRequired = false

        //Allow direction change
        if (desiredDir == -currentDir) currentDir = desiredDir
        //Use desired direction if no movement currently takes place
        if (currentDir == Vec2i.Zero) currentDir = desiredDir
        //If no desired dir is available, request one
        if (desiredDir == Vec2i.Zero) decisionRequired = true

        //MovingEntity is exactly "on" top of a node
        if (position == Vec2f(e.getTileNode.position.toVec)) {
          //Check if movement is possible and determine goal

          //Find possible movement direction
          e.getTileNode.neighbors.find(_ match {
            case thatNode: TileNode =>
              val thisNodeToThatNode = toDir(thatNode.position.toVec - e.getTileNode.position.toVec)
              thisNodeToThatNode == currentDir
            case _ => false
          }) match {
            //Possible movement direction found
            case Some(goalNode: TileNode) =>
              //Move
              val availableDistance = e.getSpeed * deltaT
              position = position + (Vec2f(currentDir) * availableDistance)

              decisionRequired = true

              //Update variables
              e.setPosition(position.x, position.y)
              e.setDesiredMovementDirection(desiredDir.x, desiredDir.y)
              e.setCurrentMovementDirection(currentDir.x, currentDir.y)
            case _ => decisionRequired = true
          }

        }
        //MovingEntity is between two nodes
        else {
          //Determine node between which the MovingEntity is located (thisNode and thatNode)
          val thisNode = e.getTileNode
          val thisNodeToEntity = toDir(position - Vec2f(thisNode.position.toVec))
          val thatNode = e.getTileNode.neighbors.find(_ match {
            case thatNode: TileNode =>
              val thisNodeToThatNode = toDir(thatNode.position.toVec - e.getTileNode.position.toVec)
              thisNodeToThatNode == thisNodeToEntity
            case _ => false
          }).get.asInstanceOf[TileNode]

          //Determine to which node the entity is heading
          val entityToThisNode = -thisNodeToEntity
          val entityToThatNode = toDir(Vec2f(thatNode.position.toVec) - position)
          val goalNode = if (currentDir == entityToThisNode) thisNode
          else if (currentDir == entityToThatNode) thatNode
          else
            throw new Exception("Error during determining to which node the entity is heading")

          //Determine distance to goal node
          val distanceToGoal = length(position - Vec2f(goalNode.position.toVec))
          val availableDistance = e.getSpeed * deltaT

          if (availableDistance < distanceToGoal) {
            position = position + (Vec2f(currentDir) * availableDistance)
          }

          //Turn according to desiredDir
          else {
            //TODO: Check this
            //decisionRequired = true
            if (e.isInstanceOf[Ghost]) {
              ai.onDecisionRequired(e)
              desiredDir = toDir(Vec2i(e.getDesiredMovementDirectionX, e.getDesiredMovementDirectionY))
            }
            //
            val turnPossible = goalNode.neighbors.find(_ match {
              case tn: TileNode =>
                desiredDir == toDir(tn.position.toVec - goalNode.position.toVec)
              case _ => false
            }).isDefined
            if (turnPossible) {
              position = Vec2f(goalNode.position.toVec) + (Vec2f(desiredDir) * (availableDistance - distanceToGoal))
              if (currentDir != desiredDir) {
                currentDir = desiredDir
              }
            } else {
              position = Vec2f(goalNode.position.toVec)
              currentDir = Vec2i.Zero
            }
          }

          //Reattatch entity if necessary
          if (length(Vec2f(thatNode.position.toVec) - position) < length(Vec2f(thisNode.position.toVec) - position))
            e.setTileNode(thatNode)

          //Update variables
          e.setPosition(position.x, position.y)
          e.setDesiredMovementDirection(desiredDir.x, desiredDir.y)
          e.setCurrentMovementDirection(currentDir.x, currentDir.y)
        }

        //Call ai handler
        if (decisionRequired) ai.onDecisionRequired(e)
        //Tell gfx component to move the object
        _inst.moveObjectTo(e.id, e.getPositionX, e.getPositionY, 0)
      }

      //Create the graphical representation form the level graph
      private def createGraphics(levelRoot: Node, inst: JavaInterface) {

        //Initially check the graphs consistency and the level size
        var minTilePos = Vec2i(Int.MaxValue)
        var maxTilePos = Vec2i(Int.MinValue)
        def preProcessNode(n: Node) = n match {
          case tn: TileNode =>
            TileGraph.checkSanity(tn)
            minTilePos = simplex3d.math.intm.IntMath.min(minTilePos, tn.position.toVec)
            maxTilePos = simplex3d.math.intm.IntMath.max(maxTilePos, tn.position.toVec)
            false
          case _ => false
        }
        breathFirstSearch.search(levelRoot, new GoalTestFunction {
          def testGoal(n: Node) = preProcessNode(n)
        })
        val levelSize = (maxTilePos - minTilePos) + Vec2i.One

        //Create temporary tile array
        val level = new Array[Array[Option[GfxTile]]](levelSize.x)
        for (i <- 0 until levelSize.x) level(i) = new Array[Option[GfxTile]](levelSize.y)
        for (i <- 0 until levelSize.x; j <- 0 until levelSize.y) level(i)(j) = None

        //Helper set, to mark already closed gaps between distant nodes (distance > 1)
        val closedGaps = collection.mutable.HashSet[(UUID, UUID)]()

        //Fill the temporary tile array (determine tile types, fill gaps, check for consistency)
        def processNode(n: Node) = n match {
          case tn: TileNode =>
            val arrayPos = tn.position.toVec - minTilePos
            //Check for consistency
            if (level(arrayPos.x)(arrayPos.y).isDefined)
              throw new Exception("More than one tile at position " + tn.position.toVec + ".")

            //Determine tile type
            level(arrayPos.x)(arrayPos.y) = Some(getGfxTileForNode(tn))

            //Fill gaps
            tn.neighbors.toList.foreach(_ match {
              case neighbor: TileNode =>
                val dir = toDir(neighbor.position.toVec - tn.position.toVec)
                //Prevent overlapping in undirected graphs
                if (!closedGaps.contains((tn.id, neighbor.id))) {
                  closedGaps += ((tn.id, neighbor.id))
                  closedGaps += ((neighbor.id, tn.id))
                  var gap = tn.position.toVec + dir
                  var gapArrayPos = gap - minTilePos
                  while (gap != neighbor.position.toVec) {
                    //Check for consistency
                    if (level(gapArrayPos.x)(gapArrayPos.y).isDefined)
                      throw new Exception("More than one tile at position " + tn.position.toVec + ".")

                    //Determine gap type
                    level(gapArrayPos.x)(gapArrayPos.y) = Some(getGfxTileForDir(dir, gap))
                    gap = gap + dir
                    gapArrayPos = gap - minTilePos
                  }
                }
              case _ =>
            })
            false
          case _ => false
        }
        breathFirstSearch.search(levelRoot, new GoalTestFunction {
          def testGoal(n: Node) = processNode(n)
        })

        //Create tiles
        var minPos = Vec3f(Float.MaxValue)
        var maxPos = Vec3f(Float.MinValue)
        for (i <- 0 until levelSize.x; j <- 0 until levelSize.y) level(i)(j).collect {
          case gfxTile =>
            val pos = Vec3f(gfxTile.position.x.toFloat, gfxTile.position.y.toFloat, 0f)
            minPos = simplex3d.math.floatm.FloatMath.min(minPos, pos)
            maxPos = simplex3d.math.floatm.FloatMath.max(maxPos, pos)
            inst.loadObject(gfxTile.geometryFile, pos.x, pos.y, pos.z, 1, 1, 1, gfxTile.id)
            inst.rotateObject(gfxTile.id, gfxTile.rotation)
        }

        //Set camera to a nice place
        val center = minPos.xy + ((maxPos.xy - minPos.xy) * 0.5f)
        inst.setCamTo(45f, 0f, 0f, center.x, -5f, 5f)

        //Find and draw pacman and ghosts
        def handleEntity(n: Node) = {
          n match {
            case p: Pacman =>
              pacman = Some(p)
              val pos = p.getTileNode.position.toVec
              p.setPosition(pos.x.toFloat, pos.y.toFloat)
              p.setCurrentMovementDirection(0, 0)
              inst.loadObject("pacman/models/pacman.dae", pos.x, pos.y, 0, .4f, .4f, .4f, p.id)
              inst.pinCamTo(p.id(), 45f, 0f, 0f, 0f, -5f, 5f)
              false
            case g: Ghost =>
              ghosts = g :: ghosts
              val pos = g.getTileNode.position.toVec
              g.setPosition(pos.x, pos.y)
              g.setCurrentMovementDirection(0, 0)
              inst.loadObject(getGfxFileForGhostNr(g.getNr), pos.x, pos.y, 0, .4f, .4f, .4f, g.id)
              false
            case g: Goodie =>
              goodies = g :: goodies
              val pos = g.getTileNode.position.toVec
              g.setPosition(pos.x, pos.y)
              inst.loadObject(foodModel, pos.x, pos.y, 0.1f, goodieRadius, goodieRadius, goodieRadius, g.id)
              false
            case p: PowerUp =>
              powerUps = p :: powerUps
              val pos = p.getTileNode.position.toVec
              p.setPosition(pos.x, pos.y)
              inst.loadObject(powerUpModel, pos.x, pos.y, 0.1f, powerUpRadius, powerUpRadius, powerUpRadius, p.id)
              false
            case _ => false
          }
        }
        breathFirstSearch.search(levelRoot, new GoalTestFunction {
          def testGoal(n: Node) = handleEntity(n)
        })
      }

      //Returns geometry files for ghosts
      private def getGfxFileForGhostNr(nr: Int): String =
        "pacman/models/ghost" + ((nr % 4) + 1).toString + ".dae"


      //Returns GfxTiles for gaps
      private def getGfxTileForDir(dir: Vec2i, pos: Vec2i): GfxTile = {
        if (dir == Vec2i.UnitX) return GfxTile(pos, 90f, "pacman/models/1x1-xy-square-line.dae")
        if (dir == -Vec2i.UnitX) return GfxTile(pos, 90f, "pacman/models/1x1-xy-square-line.dae")
        if (dir == Vec2i.UnitY) return GfxTile(pos, 0f, "pacman/models/1x1-xy-square-line.dae")
        if (dir == -Vec2i.UnitX) return GfxTile(pos, 0f, "pacman/models/1x1-xy-square-line.dae")

        throw new Exception("No grapical representation found for direction " + dir + ".")
      }

      //Returns GfxTiles for nodes
      private def getGfxTileForNode(n: TileNode): GfxTile = {
        val exits = collection.mutable.HashSet[Vec2f]()
        n.neighbors.toList.foreach(_ match {
          case tn: TileNode => exits += toDir(tn.position.toVec - n.position.toVec)
          case _ =>
        })

        //One exit
        if (exits.size == 1) {
          if (exits.contains(Vec2i.UnitX)) return GfxTile(n.position.toVec, 90f, "pacman/models/1x1-xy-square-dead-end.dae", n.id())
          if (exits.contains(Vec2i.UnitY)) return GfxTile(n.position.toVec, 180f, "pacman/models/1x1-xy-square-dead-end.dae", n.id())
          if (exits.contains(-Vec2i.UnitX)) return GfxTile(n.position.toVec, 270f, "pacman/models/1x1-xy-square-dead-end.dae", n.id())
          if (exits.contains(-Vec2i.UnitY)) return GfxTile(n.position.toVec, 0f, "pacman/models/1x1-xy-square-dead-end.dae", n.id())
        }
        //Two exits
        else if (exits.size == 2) {
          //Curve
          if (exits.contains(Vec2i.UnitX) && exits.contains(Vec2i.UnitY))
            return GfxTile(n.position.toVec, 90f, "pacman/models/1x1-xy-square-curve.dae", n.id())
          if (exits.contains(Vec2i.UnitY) && exits.contains(-Vec2i.UnitX))
            return GfxTile(n.position.toVec, 180f, "pacman/models/1x1-xy-square-curve.dae", n.id())
          if (exits.contains(-Vec2i.UnitX) && exits.contains(-Vec2i.UnitY))
            return GfxTile(n.position.toVec, 270f, "pacman/models/1x1-xy-square-curve.dae", n.id())
          if (exits.contains(-Vec2i.UnitY) && exits.contains(Vec2i.UnitX))
            return GfxTile(n.position.toVec, 0f, "pacman/models/1x1-xy-square-curve.dae", n.id())
          //Line
          if (exits.contains(Vec2i.UnitX) && exits.contains(-Vec2i.UnitX))
            return GfxTile(n.position.toVec, 90f, "pacman/models/1x1-xy-square-line.dae", n.id())
          if (exits.contains(Vec2i.UnitY) && exits.contains(-Vec2i.UnitY))
            return GfxTile(n.position.toVec, 0f, "pacman/models/1x1-xy-square-line.dae", n.id())
        }
        //Three exits
        else if (exits.size == 3) {
          if (!exits.contains(Vec2i.UnitX)) return GfxTile(n.position.toVec, 180f, "pacman/models/1x1-xy-square-t-piece.dae", n.id())
          if (!exits.contains(Vec2i.UnitY)) return GfxTile(n.position.toVec, 270f, "pacman/models/1x1-xy-square-t-piece.dae", n.id())
          if (!exits.contains(-Vec2i.UnitX)) return GfxTile(n.position.toVec, 0f, "pacman/models/1x1-xy-square-t-piece.dae", n.id())
          if (!exits.contains(-Vec2i.UnitY)) return GfxTile(n.position.toVec, 90f, "pacman/models/1x1-xy-square-t-piece.dae", n.id())
        }
        //Four exits
        else if (exits.size == 4) return GfxTile(n.position.toVec, 0f, "pacman/models/1x1-xy-square-crossing.dae", n.id())

        throw new Exception("No grapical representation found for tile at position " + n.position.toVec + " having " + n.neighbors.toList.size + " neighbours.")
      }

      //Normal Vec2i to "pacman-level-directions" (left, right, up, ...)
      private def toDir(v: Vec2i) =
        Vec2i(
          if (v.x > 0) 1 else if (v.x < 0) -1 else 0,
          if (v.y > 0) 1 else if (v.y < 0) -1 else 0
        )

      //Normal Vec2f to "pacman-level-directions" (left, right, up, ...)
      private def toDir(v: Vec2f) =
        Vec2i(
          if (v.x > 0) 1 else if (v.x < 0) -1 else 0,
          if (v.y > 0) 1 else if (v.y < 0) -1 else 0
        )

      //Stores all information necessary to create a graphical represenation for a tile
      private case class GfxTile(position: Vec2i, rotation: Float, geometryFile: String, id: UUID = UUID.randomUUID())

    }.start()
  }
}
