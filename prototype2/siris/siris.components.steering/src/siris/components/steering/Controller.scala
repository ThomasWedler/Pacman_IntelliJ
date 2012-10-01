package siris.components.steering

import siris.core.entity.Entity

import simplex3d.math._
import simplex3d.math.floatm._
import simplex3d.math.floatm.FloatMath._
import simplex3d.math.floatm.renamed._
import siris.core.svaractor.SVar
import actors.Actor
import siris.ontology.{SVarDescription, Symbols}
import siris.ontology.types.{Velocity, Matrix}
import siris.components.physics.{SetAngularVelocity, SetTransformation, ApplyImpulse}

/**
 * Created by IntelliJ IDEA.
 * User: stephan_rehfeld
 * Date: 21.09.2010
 * Time: 10:55:25
 * To change this template use File | Settings | File Templates.
 */


//@deprecated( "Use new steering framework" )
trait ControllerNormalizer {

  //@deprecated( "Use new steering framework" )
  def config( controller: Entity, keys : Set[Symbol] )
  //@deprecated( "Use new steering framework" )
  def getNormalized( key : Symbol ) : Float
}

//@deprecated( "Use new steering framework" )
class Keyboard extends ControllerNormalizer {

  //@deprecated( "Use new steering framework" )
  val values: scala.collection.mutable.Map[Symbol,Float] = scala.collection.mutable.Map()

  //@deprecated( "Use new steering framework" )
  def config( controller: Entity, keys : Set[Symbol] ) {
    for( key <- keys ) {
      values += key -> 0f
      //TODO check the hack

      val ontMem =
      if(key.name.size == 1)
        charToKey(key.name(0))
      else
        key match {
          case 'left => siris.ontology.types.Key_Left
          case 'right => siris.ontology.types.Key_Right
          case 'up => siris.ontology.types.Key_Up
          case 'down => siris.ontology.types.Key_Down
          case 'space => siris.ontology.types.Key_Space
        }

      val svar = controller.get(ontMem)
      svar.get.observe( (v) => { values += key -> (if( v == true ) 1f else 0f ) } )

        
    }
  }

  //@deprecated( "Use new steering framework" )
  def getNormalized( key: Symbol ) : Float = values( key )

  private def charToKey(c: Char): SVarDescription[scala.Boolean, scala.Boolean] = c match {
    case 'a' => siris.ontology.types.Key_a
    case 'b' => siris.ontology.types.Key_b
    case 'c' => siris.ontology.types.Key_c
    case 'd' => siris.ontology.types.Key_d
    case 'e' => siris.ontology.types.Key_e
    case 'f' => siris.ontology.types.Key_f
    case 'g' => siris.ontology.types.Key_g
    case 'h' => siris.ontology.types.Key_h
    case 'i' => siris.ontology.types.Key_i
    case 'j' => siris.ontology.types.Key_j
    case 'k' => siris.ontology.types.Key_k
    case 'l' => siris.ontology.types.Key_l
    case 'm' => siris.ontology.types.Key_m
    case 'n' => siris.ontology.types.Key_n
    case 'o' => siris.ontology.types.Key_o
    case 'p' => siris.ontology.types.Key_p
    case 'q' => siris.ontology.types.Key_q
    case 'r' => siris.ontology.types.Key_r
    case 's' => siris.ontology.types.Key_s
    case 't' => siris.ontology.types.Key_t
    case 'u' => siris.ontology.types.Key_u
    case 'v' => siris.ontology.types.Key_v
    case 'w' => siris.ontology.types.Key_w
    case 'x' => siris.ontology.types.Key_x
    case 'y' => siris.ontology.types.Key_y
    case 'z' => siris.ontology.types.Key_z
    case _ => throw new java.lang.Exception("Could not find a siris.ontology.types.Key for the character '" + c + "'!")
  }
}

//@deprecated( "Use new steering framework" )
trait Mover {
  //@deprecated( "Use new steering framework" )
  def control( user: Entity, keyValues: Map[Symbol,Float], time: Long )
}

//@deprecated( "Use new steering framework" )
class WalkMover private( val upVec: ConstVec3, val walkSpeed : Float, val turnSpeed : Float, val initialDirection: ConstVec3f = ConstVec3( 0.0f, 0.0f, -1.0f ) ) extends Mover {

  //@deprecated( "Use new steering framework" )
  var direction = initialDirection

  //@deprecated( "Use new steering framework" )
  def control( user: Entity, keyValues: Map[Symbol,Float], time: Long ) = {

    //val keyValues = keyValueProvider()

    var t : Float = time
    t = t / 100000000

    direction = Mat3x4.rotate( radians(turnSpeed) * keyValues( 'turnleft ) * t, upVec ) * Vec4f( direction.x, direction.y, direction.z, 0f )
    direction = Mat3x4.rotate( -radians(turnSpeed) * keyValues( 'turnright ) * t, upVec ) * Vec4f( direction.x, direction.y, direction.z, 0f )

    val viewPlatform = user.get(Matrix as Symbols.viewPlatform).get
    viewPlatform.get( (currentTransform) => {
      val currentPosition = ConstVec3( currentTransform.m03, currentTransform.m13, currentTransform.m23 )
      val forwardPosition = currentPosition + (direction * ( ((-keyValues( 'forward )) * t * walkSpeed ) ) )//* ((time * keyValues( 'forward ))
      val newPosition = forwardPosition + (direction * (keyValues( 'backward ) * t * walkSpeed) )
      // Rotation neu berechnen
      // Transformationsmatrix erzeugen
      // Transformationsmatrix zurueckschreiben

      val xAxis = cross( upVec, direction )

      val newTransform = ConstMat4( xAxis.x, xAxis.y, xAxis.z, 0f,  upVec.x, upVec.y, upVec.z, 0f,  direction.x, direction.y, direction.z, 0f,  newPosition.x, newPosition.y, newPosition.z, 1f )

      viewPlatform.set( newTransform )
    })
   }

}

//@deprecated( "Use new steering framework" )
object WalkMover {
  //@deprecated( "Use new steering framework" )
  def apply( upVec: ConstVec3, walkSpeed : Float, turnSpeed : Float ) : WalkMover = {
    new WalkMover( upVec, walkSpeed, turnSpeed )
  }

  //@deprecated( "Use new steering framework" )
  def apply( upVec: ConstVec3, walkSpeed : Float, turnSpeed : Float, initialDirection: ConstVec3 ) : WalkMover = {
    new WalkMover( upVec, walkSpeed, turnSpeed, initialDirection )
  }
}

class PhysicWalkMover private( val physicsComponent: Actor, val walkSpeed : Float, val turnSpeed : Float, directionVector: ConstVec3, rotateAxis: ConstVec3, walkListener : Option[Actor] = None ) extends Mover {
    var lastJump = System.nanoTime

  private var oldKeyValues = Set[Float]()

  private def moveKeyStateChange( newKeyValues: Set[Float] ) = {
    if (!oldKeyValues.isEmpty){
      if (oldKeyValues.forall( _ == 0f)){
        if ( !newKeyValues.forall( _ == 0f) )
          walkListener.collect{ case l => l ! "walk started"}
      } else {
        if ( newKeyValues.forall( _ == 0f) )
          walkListener.collect{ case l => l ! "walk stopped"}
      }
    }
    oldKeyValues = newKeyValues
  }

  //@deprecated( "Use new steering framework" )
  def control( user: Entity, keyValues: Map[Symbol,Float], time: Long ) = {
    moveKeyStateChange(keyValues.filter( _._1 != 'jump ).values.toSet)
    user.get( siris.ontology.types.Transformation ).get.get( ( t ) => {

      val direction = normalize( t * ConstVec4( directionVector.x, directionVector.y, directionVector.z, 0.0f ) ).xyz * walkSpeed * 10.0f

      user.get( Velocity ).get.get( ( v ) => {
        var xzSpeed = math.sqrt( v.x * v.x + v.z * v.z ).toFloat
        if( keyValues.contains( 'forward ) && keyValues( 'forward ) != 0.0f  ) {
          if( xzSpeed < walkSpeed ) {
            physicsComponent ! ApplyImpulse( user, Vec3f( direction.x, 0.0f, direction.z ) )
          }
        }

        if( keyValues.contains( 'backward ) && keyValues( 'backward ) != 0.0f  ) {
          if( xzSpeed < walkSpeed ) {
             val nd = direction *(-1)
             physicsComponent ! ApplyImpulse( user, Vec3f( nd.x, 0.0f, nd.z  ) )
          }
        }

        if( keyValues.contains( 'left ) && keyValues( 'left ) != 0.0f  ) {
          if( xzSpeed < walkSpeed ) {
            physicsComponent ! ApplyImpulse( user, (Mat3x4.rotateY( radians( 90.0f ) ) * Vec4f( direction.x, 0.0f, direction.z, 0.0f )).xyz )
          }
        }

        if( keyValues.contains( 'right ) && keyValues( 'right ) != 0.0f  ) {
          if( xzSpeed < walkSpeed ) {
            physicsComponent ! ApplyImpulse( user, (Mat3x4.rotateY( radians( -90.0f ) ) * Vec4f( direction.x, 0.0f, direction.z, 0.0f )).xyz  )
          }
        }
        /*if( keyValues( 'forward ) != 0.0f  ) {
          var speed = direction + v
          var xzSpeed = Math.sqrt( speed.x * speed.x + speed.z * speed.z ).asInstanceOf[Float]
          if( xzSpeed > walkSpeed ) speed = Vec3f( (speed.x / xzSpeed) * walkSpeed.asInstanceOf[Float], speed.y, (speed.z / xzSpeed) * walkSpeed )
          physicsComponent ! JBSetLinearVelocity( user, speed )
          //println( direction )
        }
        if( keyValues( 'backward ) != 0.0f  ) {
          var speed = (direction * (-1.0f)) + v
          var xzSpeed = Math.sqrt( speed.x * speed.x + speed.z * speed.z ).asInstanceOf[Float]
          if( xzSpeed > walkSpeed ) speed = Vec3f( (speed.x / xzSpeed) * walkSpeed.asInstanceOf[Float], speed.y, (speed.z / xzSpeed) * walkSpeed )
          physicsComponent ! JBSetLinearVelocity( user, speed )
          //println( direction )
        } */



       } : Unit )
        if( keyValues.contains( 'turnleft ) && keyValues( 'turnleft ) != 0.0f  ) {
          physicsComponent ! SetAngularVelocity( user,  turnSpeed * rotateAxis )
        }
        if( keyValues.contains( 'turnright ) &&  keyValues( 'turnright ) != 0.0f  ) {
          physicsComponent ! SetAngularVelocity( user,  turnSpeed * rotateAxis * (-1.0f) )
        }
        if( keyValues.contains( 'jump ) && keyValues( 'jump ) != 0.0f && (System.nanoTime - lastJump) > 1000000000  ) {
          walkListener.collect{ case l => l ! "jumped"}

          physicsComponent ! ApplyImpulse( user,  Vec3f( 0.0f, 6000.0f, 0.0f ) )
          lastJump = System.nanoTime

        }

        if( keyValues.contains( 'lookup ) && keyValues( 'lookup ) != 0.0f  ) {
          if( (t * ConstMat4( Mat3x4.rotateX( radians( 5 ) ) ) * ConstVec4( 0.0f, 0.0f, 1.0f, 0.0f )  ).y < (t * ConstVec4( 0.0f, 0.0f, 1.0f, 0.0f )).y )
          physicsComponent ! SetTransformation( user,  t * ConstMat4( Mat3x4.rotateX( radians( 5 ) ) ) )
        }
        if( keyValues.contains( 'lookdown ) && keyValues( 'lookdown ) != 0.0f  ) {
          if( (t * ConstMat4( Mat3x4.rotateX( radians( -5 ) ) ) * ConstVec4( 0.0f, 0.0f, 1.0f, 0.0f )  ).y > (t * ConstVec4( 0.0f, 0.0f, 1.0f, 0.0f )).y )
          physicsComponent ! SetTransformation( user,  t * ConstMat4( Mat3x4.rotateX( radians( -5 ) ) ) )
        }
    } : Unit)
    //val keyValues = keyValueProvider()

   }

}


object PhysicWalkMover {

  def apply( physicsComponent: Actor, walkSpeed : Float, turnSpeed : Float, directionVector: ConstVec3, rotateAxis: ConstVec3, listener : Option[Actor] ) : PhysicWalkMover = {
    new PhysicWalkMover( physicsComponent, walkSpeed, turnSpeed, directionVector, rotateAxis: ConstVec3, listener )
  }

}

//@deprecated( "Use new steering framework" )
case class Controller( controller : Entity, normalizer : ControllerNormalizer, controlledObject : Entity, mover : Mover, map : List[(Symbol,Symbol)] ) {

  //@deprecated( "Use new steering framework" )
  def as( normalizer : ControllerNormalizer ) : Controller = {
    new Controller( controller, normalizer, null, null, null )
  }

  //@deprecated( "Use new steering framework" )
  def controls( controlledObject : Entity ) : Controller = {
    new Controller( controller, normalizer, controlledObject, null, null )
  }

  //@deprecated( "Use new steering framework" )
  def as( mover : Mover ) : Controller = {
    new Controller( controller, normalizer, controlledObject, mover, null )
  }

  //@deprecated( "Use new steering framework" )
  def where( map : List[(Symbol,Symbol)] ) : Controller = {
    new Controller( controller, normalizer, controlledObject, mover, map )
  }

}

//@deprecated( "Use new steering framework" )
object Controller {
  //@deprecated( "Use new steering framework" )
  def apply( controller: Entity ) : Controller = {
    new Controller( controller, null, null, null, null )
  }

  //@deprecated( "Use new steering framework" )
  def x = {

  }

  //@deprecated( "Use new steering framework" )
  def main( args : Array[String] ) = {
    println( Controller( null ) as null.asInstanceOf[ControllerNormalizer] controls null as null.asInstanceOf[Mover] where 'a -> 'up :: 'b -> 'down :: Nil )
  }

}