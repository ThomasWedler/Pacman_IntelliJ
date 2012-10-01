package siris.components.physics.jbullet

import xml.Node
import simplex3d.math.floatm._
import javax.vecmath.Vector3f
import com.bulletphysics.dynamics.{RigidBodyConstructionInfo, RigidBody}
import simplex3d.math.floatm.FloatMath._
import siris.components.physics.jbullet.JBulletMath._
import com.bulletphysics.linearmath.{Transform, DefaultMotionState}
import siris.core.entity.description.NamedSValList
import collection.mutable.HashMap
import com.bulletphysics.collision.shapes.{CollisionShape, ConvexHullShape}
import siris.components.physics.PhysicsException

//Global Types
import siris.ontology.{types => gt}
//Helps shortening the javax and jbullet.linearmath code
import siris.components.physics.jbullet.{JBulletMath => m}


/**
 * User: martin
 * Date: Nov 29, 2010
 */

/**
 * Loads RigidBodys from collada files and manages loaded data.
 */
object JBulletColladaLoader {

  /**
   *  Identifies a RigidBody stored in a collada file.
   */
  private case class RigidBodyUrl(fileName: String, objectId: String)

  /**
   *  Stores all properties needed to create a RigidBody
   */
  private case class RigidBodyInfo(shape: CollisionShape, mass: Float, centerOfMassOffset: Transform){
    var restitution: Option[Float] = None
    var staticFriction: Option[Float] = None
    var linearVelocity: Option[Vector3f] = None
    var angularVelocity: Option[Vector3f] = None

    /**
     *  Creates a RigidBody from this RigidBodyInfo
     */
    def createRigidBody: JBulletRigidBody = {

      val inertia =
        if(mass != 0f) {
          val tempInertia = new Vector3f
          shape.calculateLocalInertia(mass, tempInertia)
          tempInertia
        }
        else m.ZeroVec

      val rb = new JBulletRigidBody(new RigidBodyConstructionInfo(
        mass,
        new DefaultMotionState(m.Idendity, centerOfMassOffset),
        shape,
        inertia))

      restitution.collect{case r => rb.setRestitution(r)}
      staticFriction.collect{case f => rb.setFriction(f)}
      linearVelocity.collect{case lv => rb.setLinearVelocity(lv)}
      angularVelocity.collect{case av => rb.setAngularVelocity(av)}

      rb
    }
  }

  /**
   *   Stores all loaded RigidBodies.
   */
  private val loaded = new HashMap[RigidBodyUrl, RigidBodyInfo]()

  /**
   *    Returns the RigidBody described in the SValList.
   * Loades RigidBodies only once from disc.
   */
  def get(tcps: NamedSValList): JBulletRigidBody = {

    val url = RigidBodyUrl(tcps.firstValueFor(gt.ColladaFile), tcps.getFirstValueForOrElse(gt.ColladaObjectId)(""))
    if(!loaded.contains(url)) loaded += (url -> load(url))
    loaded(url).createRigidBody
  }

  /**
   *  Loads a RigidBodyInfo from a collada file.
   */
  private def load(url: RigidBodyUrl): RigidBodyInfo = {

    val fileName = url.fileName
    val objectId = url.objectId

    //Load the collada file
    val dae = xml.XML.loadFile(fileName)

    //Returns to value of an node option or throws an exception
    def get(o: Option[Node]): Node = {
       o match {
         case Some(node) => node
         case None => throw PhysicsException("Error while reading from collada file '" + fileName + "'.")
       }
    }

    //Returns a specific library_geometries child
    def getGeometryNodeById(id: String) = {
      get(((dae \ "library_geometries") \ "geometry").find( (n: Node) => (n \ "@id").toString == id))
    }

    def getMaterialNodeById(id: String) = {
      get(((dae \ "library_physics_materials") \ "physics_material").find( (n: Node) => (n \ "@id").toString == id))
    }

    //Extract the RigidBody entries from library_physics_scenes
    val rigidBodyNodes = (dae \ "library_physics_scenes") \\ "instance_rigid_body"

    val instanceRbNode =
    //Search the actual rb
    if(objectId != ""){
      rigidBodyNodes.find( (n: Node) => (n \ "@target").toString == ("#" + objectId) ) match {
        case Some(node) => node
        case None => throw PhysicsException("ObjectId '" + objectId + "' not found in collada file '" + fileName + "'.")
      }
    }
    //Take the first rb
    else {
      rigidBodyNodes.headOption match {
        case Some(node) => node
        case None => throw PhysicsException("No rigidbody found in collada file '" + fileName + "'.")
      }
    }


    val sid = (instanceRbNode \ "@body").toString

    //Get the rb node from library_physics_models
    val rbNode = get(((dae \ "library_physics_models") \\ "rigid_body").find( (n: Node) => (n \ "@sid").toString == sid))


    val shapeNode = get((rbNode \\ "shape").find( (n: Node) => n.child.exists(_.label == "instance_geometry") ))

    val instanceGeometryURL = ((shapeNode \ "instance_geometry") \ "@url").toString.replaceAll("#", "")

    val geometryNode = getGeometryNodeById(instanceGeometryURL)
    val convexMeshNode = get((geometryNode \ "convex_mesh").headOption)

    val sourceNode = convexMeshNode.attribute("convex_hull_of") match {
      case Some(id) => get((getGeometryNodeById(id.toString.replaceAll("#", "")) \ "mesh" \ "source").headOption)
      case None => get((convexMeshNode \ "source").headOption)
    }

    //Minimal test if data is what i think it is
    val accessorNode = get((sourceNode \ "technique_common" \ "accessor").headOption)
    if( (!accessorNode.attribute("stride").isDefined) || (accessorNode.attribute("stride").get.toString.toInt != 3))
      throw PhysicsException("Unsupported physical geomety in collada file '" + fileName + "'.")
    //Minimal test if data is what i think it is END

    val floatArrayNode = get((sourceNode \ "float_array").headOption)

    val rawData = for(f <- floatArrayNode.child.head.toString.split(" ")) yield f.toFloat
    val floatArrayData = myZip(rawData.toList)

    //Transform shape
    var dataTransform = (shapeNode \ "rotate").headOption match {
      case Some(node) => (for(f <- node.child.head.toString.split(" ")) yield f.toFloat).toList match {
        case x :: y :: z :: angle :: Nil => Mat3x4f.rotate(radians(angle), Vec3f(x, y, z))
        case _ => Mat3x4f.Identity
      }
      case None => Mat3x4f.Identity
    }

    val constDataTransform = (shapeNode \ "translate").headOption match {
      case Some(node) => (for(f <- node.child.head.toString.split(" ")) yield {f.toFloat}).toList match {
        case x :: y :: z :: Nil =>  ConstMat4f(Mat3x4f.translate(Vec3f(x,y,z))) * ConstMat4f(dataTransform)
        case _ => ConstMat4f(dataTransform)
      }
      case None => ConstMat4f(dataTransform)
    }

    val mass = get((rbNode \\ "mass").headOption).child.head.toString.toFloat

    val transformedFloatArrayData =
    if(mass != 0) {
      for(point <- floatArrayData) yield {
        val s3dVec4 =  /*ConstMat4f(dataTransform)  * */ ConstVec4f(point);
        new Vector3f(s3dVec4.x, s3dVec4.y, s3dVec4.z)
      }
    }
    else {
      for(point <- floatArrayData) yield {
        val s3dVec4 =  constDataTransform  * ConstVec4f(point)
        new Vector3f(s3dVec4.x, s3dVec4.y, s3dVec4.z)
      }
    }
    //Transform shape END

    //Create RigidBody
    val dataContainer = new com.bulletphysics.util.ObjectArrayList[javax.vecmath.Vector3f]()
    transformedFloatArrayData.foreach((value) => dataContainer.add(value))
    val shape = new ConvexHullShape(dataContainer)

    var centerOfMassOffset = JBulletConverters.transformConverter revert Mat4x4f(inverse(constDataTransform))

    if(mass == 0) centerOfMassOffset = m.Idendity


    var inertia = (rbNode \\ "inertia").headOption match {
      case Some(node) => (for(f <- node.child.head.toString.split(" ")) yield f.toFloat).toList match {
        case x :: y :: z :: Nil => val tempVec = new Vector3f(); shape.calculateLocalInertia(mass, tempVec); tempVec//new Vector3f(x, y, z)
        case _ => val tempVec = new Vector3f(); shape.calculateLocalInertia(mass, tempVec); tempVec
      }
      case None => val tempVec = new Vector3f(); shape.calculateLocalInertia(mass, tempVec); tempVec
    }

    val rbi = RigidBodyInfo(shape, mass, centerOfMassOffset)
//    val rb = if(mass != 0) new JBulletRigidBody(
//              new RigidBodyConstructionInfo(
//                  mass,
//                  new DefaultMotionState(new Transform(IdendityMat), centerOfMassOffset),
//                  shape,
//                  inertia )) else
//    new JBulletRigidBody(
//              new RigidBodyConstructionInfo(
//                  mass,
//                  new DefaultMotionState(new Transform(IdendityMat), centerOfMassOffset),
//                  shape,
//                  inertia ))

    //Set instance material properties if present
    (rbNode \\ "instance_physics_material").headOption match {
      case Some(node) => {
        val instancePhysicsMaterialUrl = (node \ "@url").toString.replaceAll("#", "")
        val physicsMaterialNode = getMaterialNodeById(instancePhysicsMaterialUrl)
        //Set restitution and friction if present
        (physicsMaterialNode \\ "restitution").headOption.collect{case node => rbi.restitution = Some(node.child.head.toString.toFloat)}
        (physicsMaterialNode \\ "static_friction").headOption.collect{case node => rbi.staticFriction = Some(node.child.head.toString.toFloat)}
      }
      case None => 
    }

    //Set velocity and angular velocity if present  
    (instanceRbNode \\ "velocity").headOption.collect{
      case node => (for(f <- node.child.head.toString.split(" ")) yield f.toFloat).toList match {
        case x :: y :: z :: Nil => rbi.linearVelocity = Some(new Vector3f(x, y, z))
        case _ =>
      }
    }

    (instanceRbNode \\ "angular_velocity").headOption.collect{
      case node => (for(f <- node.child.head.toString.split(" ")) yield f.toFloat).toList match {
        case x :: y :: z :: Nil => rbi.angularVelocity= Some(new Vector3f(x, y, z))
        case _ =>
      }
    }

    rbi
  }


  def myZip(l: List[Float]): List[Vec4f] = l match {
    case a :: b :: c :: tail => Vec4f(a, b, c, 1f) :: myZip(tail)
    case _ => Nil
  }

}