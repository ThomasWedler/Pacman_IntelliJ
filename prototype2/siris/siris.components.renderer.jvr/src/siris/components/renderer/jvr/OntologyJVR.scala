package siris.components.renderer.jvr

import de.bht.jvr.math.Matrix4
import simplex3d.math.floatm.ConstMat4f
import siris.ontology.Symbols
import siris.ontology.types._
import siris.components.renderer.types.ManipulatorList
import siris.core.entity.typeconversion.{ConvertibleTrait, Converter}

/**
 * This objects represents the OntologyJVR internal ontology.
 *
 * @author Stephan Rehfeld
 */

@deprecated( "Will be removed by a generated ontology in near future!", "Hackathon Berlin" )
object OntologyJVR {

  object light {
    object color {
      val diffuse = Color as Symbols.diffuseColor
      val specular = Color as Symbols.specularColor
    }
    object attenuation {
      val constant = Real as Symbols.constantAttenuation
      val linear = Real as Symbols.linearAttenuation
      val quadratic = Real as Symbols.quadraticAttenuation
    }
    val spotCutOff = Real as Symbols.spotCutOff
    val spotExponent = Real as Symbols.spotExponent

    object shadow {
      val cast = Boolean as Symbols.castShadow
      val bias = Real as Symbols.shadowBias
    }
  }

  val subElement = String as Symbols.subElement
  val parentElement = Entity as Symbols.parentElement

  val name = String as Symbols.name

  val transformation = Matrix as Symbols.transformation
    
  /**
   * This member represents a transform in type of OntologyJVR.
   */
  val transform = Matrix as Symbols.transformation createdBy (new de.bht.jvr.core.Transform)


}

