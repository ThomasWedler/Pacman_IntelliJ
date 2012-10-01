package siris.components.physics

import simplex3d.math.floatm.renamed.{ConstVec3, ConstMat4, Mat3x4}
import siris.ontology.Symbols
import runtime.{RichShort, RichFloat}
import siris.core.entity.description.{SValSeq, SVal, Semantics, Aspect}
import siris.core.entity.typeconversion.ConvertibleTrait

//Global Types
import siris.ontology.{types => gt}

/**
 * User: dwiebusch & mfischbach
 * Date: 127.0.0.1
 * Time: 13:37
 */

/**
 * Base class for all CreateParameterSets send to physics components
 */
abstract class PhysicsAspect(aspectType : SVal[Semantics], targets : List[Symbol] = Nil)
  extends Aspect(Symbols.physics, aspectType, targets)
{
  def getFeatures =
    Set( gt.Mass, gt.Transformation, gt.Velocity, gt.Acceleration )
}

/**
 * Defines implicit conversion for Either
 */
object ImplicitEitherConversion {
  implicit def left2Either[A,B](a:A):Either[A,B] = Left(a)
  implicit def right2Either[A,B](b:B):Either[A,B] = Right(b)
}

/**
 * Describes the three coordinate axis.
 */
object Axis extends Enumeration {
  val X = Value("XAxis")
  val Y = Value("YAxis")
  val Z = Value("ZAxis")
}

/**
 * Describes properties all physically represented solid body entities have.
 *
 * The default values mentioned in this comment below are applied by the respective components and are
 *          not written here to distinguish in certain cases if values have been passed or not.
 *
 * @param transform   The initial transform. This can ''either'' be a matrix or a vector specifying the initial position only.
 *                    <br />'''If no transformation is provided, the transformation has to come from another component!'''
 *                    <br />Import ''siris.components.physics.ImplicitEitherConversion._'' to have implicit conversions from
 *                    ConstMat4 or ConstVec3 to Either.
 * @param mass        Default = 1f. The mass of the physical entity. If mass is 0f the entity is considered to be static.
 * @param gravity     The gravity that effects this entity. If set it overrides the global gravity for this entity.
 * @param restitution Default = 0f. Determines the impule that a rigidbody has after a collision compared to the impule before.
 *                    <br />Values go from 0.0f to 1.0f. In the simulation, the resitution of the collision partner is also considered.
 * @param linDamping  Default = 0f. Damping for linear motions. Values go from 0.0f to 1.0f.
 * @param anDamping   Default = 0f. Damping for angular motions. Values go from 0.0f to 1.0f.
 * @param anFactor    Default = 1f. Multiplier for angular forces and impules.
 * @param targets     Names of components that shall receive this Aspect (see [[siris.core.entity.description.Aspect]]).
 */
case class PhysBaseProps (transform: Either[ConstMat4, ConstVec3] = null, mass: RichFloat = null, gravity: ConstVec3 = null, restitution: RichFloat = null,
                         linDamping: RichFloat = null, anDamping: RichFloat = null, anFactor: RichFloat = null, collisionGroup: RichShort = null, collidesWith: RichShort = null) {

  def and(that: SVal[_]) = {

    var result = new SValSeq()

    if (transform != null) result = result and {transform match {
      case Left(p) => gt.Transformation(p)
      case Right(p) => gt.Transformation(ConstMat4(Mat3x4.translate(p)))
    }}
    if(mass != null) result = result and gt.Mass(mass.self.asInstanceOf[Float])
    if(gravity != null) result = result and gt.Gravity(gravity)
    if(restitution != null) result = result and gt.Restitution(restitution.self.asInstanceOf[Float])
    if(linDamping != null) result = result and gt.LinearDamping(linDamping.self.asInstanceOf[Float])
    if(anDamping != null) result = result and gt.AngularDamping(anDamping.self.asInstanceOf[Float])
    if(anFactor != null) result = result and gt.AngularFactor(anFactor.self.asInstanceOf[Float])
    if(collisionGroup != null) result = result and gt.CollisionGroup(collisionGroup.self.asInstanceOf[Short])
    if(collidesWith != null) result = result and gt.CollidesWith(collidesWith.self.asInstanceOf[Short])

    result and that
  }

  def getProvidings: Set[ConvertibleTrait[_]] =
    if(transform != null) Set(gt.Transformation, gt.Mass, gt.Velocity, gt.Acceleration)
    else Set(gt.Mass, gt.Velocity, gt.Acceleration)
}

/**
 * Describes a simple physical sphere.
 *
 * @param radius      The radius.
 * @param upVector    If it is set, the sphere's orientation will always be aligned with the move direction
 *                    by using the direction of the sphere's current velocity, the upVector and their cross product.
 * @param transform   The initial transform. This can ''either'' be a matrix or a vector specifying the initial position only.
 *                    <br />'''If no transformation is provided, the transformation has to come from another component!'''
 *                    <br />Import ''siris.components.physics.ImplicitEitherConversion._'' to have implicit conversions from
 *                    ConstMat4 or ConstVec3 to Either.
 * @param mass        Default = 1f. The mass of the physical entity. If mass is 0f the entity is considered to be static.
 * @param gravity     The gravity that effects this entity. If set it overrides the global gravity for this entity.
 * @param restitution Default = 0f. Determines the impule that a rigidbody has after a collision compared to the impule before.
 *                    <br />Values go from 0.0f to 1.0f. In the simulation, the resitution of the collision partner is also considered.
 * @param linDamping  Default = 0f. Damping for linear motions. Values go from 0.0f to 1.0f.
 * @param anDamping   Default = 0f. Damping for angular motions. Values go from 0.0f to 1.0f.
 * @param anFactor    Default = 1f. Multiplier for angular forces and impules.
 * @param targets     Names of components that shall receive this Aspect (see [[siris.core.entity.description.Aspect]]).
 *
 * @see [[siris.components.physics.PhysBaseProps]]
 */
case class PhysSphere(radius: Float = 1f, upVector: ConstVec3 = null,
                      transform: Either[ConstMat4, ConstVec3] = null, mass: RichFloat = null,
                      gravity: ConstVec3 = null, restitution: RichFloat = null,
                      linDamping: RichFloat = null, anDamping: RichFloat = null,
                      anFactor: RichFloat = null, collisionGroup: RichShort = null, collidesWith: RichShort = null,
                      override val targets : List[Symbol] = Nil) extends PhysicsAspect(Symbols.sphere) {

  val pbp = PhysBaseProps(transform, mass, gravity, restitution, linDamping, anDamping, anFactor, collisionGroup, collidesWith)

  def getCreateParams = addCVars{
    if(upVector == null) pbp and gt.Radius(radius)
    else pbp and gt.Radius(radius) and gt.UpVector(upVector)
  }

  override def getProvidings =
    pbp.getProvidings
}

/**
 * Describes a physical object with the shape of a capsule.
 *
 * @param radius      The radius.
 * @param height      The height.
 * @param align       The axis along which the capsule will be aligned. The rotational symetry axis of the
 *                    capsule will be parallel to this axis. Use `siris.components.physics.Axis`.
 * @param transform   The initial transform. This can ''either'' be a matrix or a vector specifying the initial position only.
 *                    <br />'''If no transformation is provided, the transformation has to come from another component!'''
 *                    <br />Import ''siris.components.physics.ImplicitEitherConversion._'' to have implicit conversions from
 *                    ConstMat4 or ConstVec3 to Either.
 * @param mass        Default = 1f. The mass of the physical entity. If mass is 0f the entity is considered to be static.
 * @param gravity     The gravity that effects this entity. If set it overrides the global gravity for this entity.
 * @param restitution Default = 0f. Determines the impule that a rigidbody has after a collision compared to the impule before.
 *                    <br />Values go from 0.0f to 1.0f. In the simulation, the resitution of the collision partner is also considered.
 * @param linDamping  Default = 0f. Damping for linear motions. Values go from 0.0f to 1.0f.
 * @param anDamping   Default = 0f. Damping for angular motions. Values go from 0.0f to 1.0f.
 * @param anFactor    Default = 1f. Multiplier for angular forces and impules.
 * @param targets     Names of components that shall receive this Aspect (see [[siris.core.entity.description.Aspect]]).
 *
 * @see [[siris.components.physics.PhysBaseProps]], `siris.components.physics.Axis`
 */
case class PhysCapsule( radius: Float = 1f, height: Float= 2f, align: Enumeration#Value = Axis.Y,
                        transform: Either[ConstMat4, ConstVec3] = null, mass: RichFloat = null,
                        gravity: ConstVec3 = null, restitution: RichFloat = null,
                        linDamping: RichFloat = null, anDamping: RichFloat = null,
                        anFactor: RichFloat = null,
                        override val targets : List[Symbol] = Nil) extends PhysicsAspect(Symbols.capsule) {

  val pbp = PhysBaseProps(transform, mass, gravity, restitution, linDamping, anDamping, anFactor)

  def getCreateParams =
    addCVars{pbp and gt.Radius(radius) and gt.Height(height) and gt.Alignment(align)}

  override def getProvidings =
    pbp.getProvidings
}

/**
 * Describes a physical object with the shape of a cylinder.
 *
 * @param radius      The radius.
 * @param height      The height.
 * @param align       The axis along which the cylinder will be aligned. The rotational symetry axis of the
 *                    <br />cylinder will be parallel to this axis. Use [[siris.components.physics.Axis]].
 * @param transform   The initial transform. This can ''either'' be a matrix or a vector specifying the initial position only.
 *                    <br />'''If no transformation is provided, the transformation has to come from another component!'''
 *                    <br />Import ''siris.components.physics.ImplicitEitherConversion._'' to have implicit conversions from
 *                    ConstMat4 or ConstVec3 to Either.
 * @param mass        Default = 1f. The mass of the physical entity. If mass is 0f the entity is considered to be static.
 * @param gravity     The gravity that effects this entity. If set it overrides the global gravity for this entity.
 * @param restitution Default = 0f. Determines the impule that a rigidbody has after a collision compared to the impule before.
 *                    <br />Values go from 0.0f to 1.0f. In the simulation, the resitution of the collision partner is also considered.
 * @param linDamping  Default = 0f. Damping for linear motions. Values go from 0.0f to 1.0f.
 * @param anDamping   Default = 0f. Damping for angular motions. Values go from 0.0f to 1.0f.
 * @param anFactor    Default = 1f. Multiplier for angular forces and impules.
 * @param targets     Names of components that shall receive this Aspect (see [[siris.core.entity.description.Aspect]]).
 *
 * @see [[siris.components.physics.PhysBaseProps]], `siris.components.physics.Axis`, [[file:///Users/martin/Repositories/siris/source/prototype2/siris/target/scala_2.9.0-1/doc/main/api/siris/components/physics/Axis$.html Axis]]
 */
case class PhysCylinder(radius: Float = 1f, height: Float= 2f, align: Enumeration#Value = Axis.Y,
                        transform: Either[ConstMat4, ConstVec3] = null, mass: RichFloat = null,
                        gravity: ConstVec3 = null, restitution: RichFloat = null,
                        linDamping: RichFloat = null, anDamping: RichFloat = null,
                        anFactor: RichFloat = null,
                        override val targets : List[Symbol] = Nil) extends PhysicsAspect(Symbols.cylinder) {

  val pbp = PhysBaseProps(transform, mass, gravity, restitution, linDamping, anDamping, anFactor)

  def getCreateParams =
    addCVars{pbp and gt.Radius(radius) and gt.Height(height) and gt.Alignment(align)}

  override def getProvidings =
    pbp.getProvidings
}

/**
 * Describes a physical object with the shape of a plane.
 *
 * @param normal      The normal of the plane.
 * @param thickness   The thickness of the plane.
 * @param transform   The initial transform. This can ''either'' be a matrix or a vector specifying the initial position only.
 *                    <br />'''If no transformation is provided, the transformation has to come from another component!'''
 *                    <br />Import ''siris.components.physics.ImplicitEitherConversion._'' to have implicit conversions from
 *                    ConstMat4 or ConstVec3 to Either.
 * @param mass        Default = 1f. The mass of the physical entity. If mass is 0f the entity is considered to be static.
 * @param gravity     The gravity that effects this entity. If set it overrides the global gravity for this entity.
 * @param restitution Default = 0f. Determines the impule that a rigidbody has after a collision compared to the impule before.
 *                    <br />Values go from 0.0f to 1.0f. In the simulation, the resitution of the collision partner is also considered.
 * @param linDamping  Default = 0f. Damping for linear motions. Values go from 0.0f to 1.0f.
 * @param anDamping   Default = 0f. Damping for angular motions. Values go from 0.0f to 1.0f.
 * @param anFactor    Default = 1f. Multiplier for angular forces and impules.
 * @param targets     Names of components that shall receive this Aspect (see [[siris.core.entity.description.Aspect]]).
 *
 * @see [[siris.components.physics.PhysBaseProps]]
 */
case class PhysPlane( normal: ConstVec3, thickness: Float = 1f,
                      transform: Either[ConstMat4, ConstVec3] = null, mass: RichFloat = null,
                      gravity: ConstVec3 = null, restitution: RichFloat = null,
                      linDamping: RichFloat = null, anDamping: RichFloat = null,
                      anFactor: RichFloat = null,
                      override val targets : List[Symbol] = Nil) extends PhysicsAspect(Symbols.plane) {

  val pbp = PhysBaseProps(transform, mass, gravity, restitution, linDamping, anDamping, anFactor)

  def getCreateParams =
    addCVars{pbp and gt.Normal(normal) and gt.Thickness(thickness)}

  override def getProvidings =
    pbp.getProvidings
}

/**
 * Describes a physical object with the shape of a box.
 *
 * @param halfExtends A vector which values describe the half the range of the box along the respective axis.
 * @param transform   The initial transform. This can ''either'' be a matrix or a vector specifying the initial position only.
 *                    <br />'''If no transformation is provided, the transformation has to come from another component!'''
 *                    <br />Import ''siris.components.physics.ImplicitEitherConversion._'' to have implicit conversions from
 *                    ConstMat4 or ConstVec3 to Either.
 * @param mass        Default = 1f. The mass of the physical entity. If mass is 0f the entity is considered to be static.
 * @param gravity     The gravity that effects this entity. If set it overrides the global gravity for this entity.
 * @param restitution Default = 0f. Determines the impule that a rigidbody has after a collision compared to the impule before.
 *                    <br />Values go from 0.0f to 1.0f. In the simulation, the resitution of the collision partner is also considered.
 * @param linDamping  Default = 0f. Damping for linear motions. Values go from 0.0f to 1.0f.
 * @param anDamping   Default = 0f. Damping for angular motions. Values go from 0.0f to 1.0f.
 * @param anFactor    Default = 1f. Multiplier for angular forces and impules.
 * @param targets     Names of components that shall receive this Aspect (see [[siris.core.entity.description.Aspect]]).
 *
 * @see [[siris.components.physics.PhysBaseProps]]
 */
case class PhysBox( halfExtends: ConstVec3,
                    transform: Either[ConstMat4, ConstVec3] = null, mass: RichFloat = null,
                    gravity: ConstVec3 = null, restitution: RichFloat = null,
                    linDamping: RichFloat = null, anDamping: RichFloat = null,
                    anFactor: RichFloat = null,
                    override val targets : List[Symbol] = Nil) extends PhysicsAspect(Symbols.box) {

  val pbp = PhysBaseProps(transform, mass, gravity, restitution, linDamping, anDamping, anFactor)

  def getCreateParams =
    addCVars{pbp and gt.HalfExtends(halfExtends)}

  override def getProvidings =
    pbp.getProvidings
}

/**
 * Describes a physical object loaded from a collada file.
 *
 * All passed properties will overwrite respective properties defined in the file.
 *
 * @param colladaFile         The collada file.
 * @param colladaObjectId     The id of the object in the collada file. If the id is an empty string the first rigidbody
 *                            in the collada file is loaded.
 * @param transform   The initial transform. This can ''either'' be a matrix or a vector specifying the initial position only.
 *                    <br />'''If no transformation is provided, the transformation has to come from another component!'''
 *                    <br />Import ''siris.components.physics.ImplicitEitherConversion._'' to have implicit conversions from
 *                    ConstMat4 or ConstVec3 to Either.
 * @param mass        Default = 1f. The mass of the physical entity. If mass is 0f the entity is considered to be static.
 * @param gravity     The gravity that effects this entity. If set it overrides the global gravity for this entity.
 * @param restitution Default = 0f. Determines the impule that a rigidbody has after a collision compared to the impule before.
 *                    <br />Values go from 0.0f to 1.0f. In the simulation, the resitution of the collision partner is also considered.
 * @param linDamping  Default = 0f. Damping for linear motions. Values go from 0.0f to 1.0f.
 * @param anDamping   Default = 0f. Damping for angular motions. Values go from 0.0f to 1.0f.
 * @param anFactor    Default = 1f. Multiplier for angular forces and impules.
 * @param targets     Names of components that shall receive this Aspect (see [[siris.core.entity.description.Aspect]]).
 *
 * @see [[siris.components.physics.PhysBaseProps]]
 */
case class PhysFromColladaFile( colladaFile: String, colladaObjectId: String = "",
                                transform: Either[ConstMat4, ConstVec3] = null, mass: RichFloat = null,
                                gravity: ConstVec3 = null, restitution: RichFloat = null,
                                linDamping: RichFloat = null, anDamping: RichFloat = null,
                                anFactor: RichFloat = null,
                                override val targets : List[Symbol] = Nil) extends PhysicsAspect(Symbols.colladaFile) {

  val pbp = PhysBaseProps(transform, mass, gravity, restitution, linDamping, anDamping, anFactor)

  def getCreateParams =
    addCVars{pbp and gt.ColladaFile(colladaFile) and gt.ColladaObjectId(colladaObjectId)}

  override def getProvidings =
    pbp.getProvidings
}

/**
 * Describes a particle with a mass that does not collide with anything.
 */
case class PhysParticle(  transform: Either[ConstMat4, ConstVec3] = null, mass: RichFloat = null,
                          linDamping: RichFloat = null,
                          override val targets : List[Symbol] = Nil) extends PhysicsAspect(Symbols.particle) {

  def getCreateParams =
    addCVars{
      var result = new SValSeq()

      if (transform != null) result = result and {
        transform match {
          case Left(p) => gt.Transformation(p)
          case Right(p) => gt.Transformation(ConstMat4(Mat3x4.translate(p)))
        }
      }
      if (mass != null) result = result and gt.Mass(mass.self.asInstanceOf[Float])
      if (linDamping != null) result = result and gt.LinearDamping(linDamping.self.asInstanceOf[Float])
      result
    }

  override def getProvidings =
    if(transform != null) Set(gt.Transformation, gt.Mass, gt.Velocity, gt.Acceleration)
    else Set(gt.Mass, gt.Velocity, gt.Acceleration)
}



