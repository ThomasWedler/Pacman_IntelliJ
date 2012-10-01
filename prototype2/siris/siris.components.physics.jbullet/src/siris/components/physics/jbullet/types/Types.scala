package siris.components.physics.jbullet.types

import siris.ontology.{SVarDescription, Symbols}



object Acceleration extends SVarDescription( siris.components.physics.jbullet.types.Vector3 as Symbols.acceleration )

object Gravity extends SVarDescription( siris.components.physics.jbullet.types.Vector3 as Symbols.gravity )

object HalfExtends extends SVarDescription( siris.components.physics.jbullet.types.Vector3 as Symbols.halfExtends )

object Impulse extends SVarDescription( siris.components.physics.jbullet.types.Vector3 as Symbols.impulse )

object Normal extends SVarDescription( siris.components.physics.jbullet.types.Vector3 as Symbols.normal )

object Transformation extends SVarDescription[com.bulletphysics.linearmath.Transform, simplex3d.math.floatm.renamed.Mat4x4]( siris.ontology.types.Matrix as Symbols.transformation createdBy(new com.bulletphysics.linearmath.Transform(new javax.vecmath.Matrix4f(1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1))) )

object Vector3 extends SVarDescription[javax.vecmath.Vector3f, simplex3d.math.floatm.renamed.Vec3]( siris.ontology.types.Vector3 as Symbols.vector3 createdBy(new javax.vecmath.Vector3f) )
object Velocity extends SVarDescription( siris.components.physics.jbullet.types.Vector3 as Symbols.velocity )