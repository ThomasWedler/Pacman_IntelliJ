package siris.components.renderer.jvr

import siris.ontology.SVarDescription
import java.io.File
import de.bht.jvr.core.{ShaderProgram, ShaderMaterial}
import de.bht.jvr.math.Vector2
import de.bht.jvr.core.uniforms.{UniformVector2, UniformBool, UniformInt, UniformFloat}
import siris.core.entity.Entity
import siris.core.svaractor.SVar
import siris.core.entity.typeconversion.ConvertibleTrait

/**
 * Created by IntelliJ IDEA.
 * User: stephan_rehfeld
 * Date: 18.07.11
 * Time: 13:31
 * To change this template use File | Settings | File Templates.
 */

class ShaderEffect( name : String ) {

  var renderPasses = List[RenderPass]()
  private var shaderMaterial : Option[ShaderMaterial] = None

  def has( renderPass : RenderPass ) : ShaderEffect = {
    renderPasses = renderPasses ::: renderPass :: Nil
    this
  }

  def and( renderPass : RenderPass ) : ShaderEffect = has( renderPass )

  def getShaderMaterial : ShaderMaterial = {
    if( !shaderMaterial.isDefined ) {
      val sm = new ShaderMaterial( )
      for( renderPass <- renderPasses ) {
        sm.setShaderProgram( renderPass.name, renderPass.getShaderProgram )
        for( (image,name) <- renderPass.images ) sm.setTexture( renderPass.name, name, ResourceManager.loadTexture( new File( image ) ) )
        for( uniform <- renderPass.uniformList ) {
          val uniformValue = uniform.value match {
            case v : Float =>
              new UniformFloat( v )
            case v : Int =>
              new UniformInt( v )
            case v : Boolean =>
              new UniformBool( v )
            case v : Vector2 =>
              new UniformVector2( v )
            case v : Seq[_] =>
              sm.setUniform( renderPass.name, uniform.name + "_size", new UniformInt( v.size ) )
              v.head match {
                case h : Vector2 =>
                  new UniformVector2( v.asInstanceOf[Seq[Vector2]].toArray : _* )
              }
          }
          sm.setUniform( renderPass.name, uniform.name, uniformValue )
        }
      }

      shaderMaterial = Some( sm )
    }
    shaderMaterial.get
  }

  def bindShaderMaterialToEntity( entity : Entity ) {
    for( renderPass <- renderPasses ) {
      for( uniformManager <- renderPass.uniformList ) {
        if( uniformManager.ontologyMember.isDefined )
          uniformManager.value match {
            case v : Float =>
              entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Float]].observe( ( v : Float ) => { getShaderMaterial.setUniform( renderPass.name, uniformManager.name, new UniformFloat( v ) ) }  )
            case v : Int =>
              entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Int]].observe( ( v : Int ) => { getShaderMaterial.setUniform( renderPass.name, uniformManager.name, new UniformInt( v ) ) }  )
            case v : Boolean =>
              entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Boolean]].observe( ( v : Boolean ) => { getShaderMaterial.setUniform( renderPass.name, uniformManager.name, new UniformBool( v ) ) }  )
            case v : Vector2 =>
              entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Vector2]].observe( ( v : Vector2 ) => { getShaderMaterial.setUniform( renderPass.name, uniformManager.name, new UniformVector2( v ) ) }  )
            case v : Seq[_] =>
              v.head match {
                case h : Vector2 =>
                  entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Seq[Vector2]]].observe( ( v : Seq[Vector2] ) => {
                    getShaderMaterial.setUniform( renderPass.name, uniformManager.name, new UniformVector2( v.toArray : _* ) )
                    getShaderMaterial.setUniform( renderPass.name, uniformManager.name + "_size", new UniformInt( v.size ) )
                  }  )
              }

          }
      }
    }
  }

  def getSVarDescriptions : List[SVarDescription[_,_]] = {
    var sVarDescriptions = List[SVarDescription[_,_]]()
    for( renderPass <- this.renderPasses ) {
      for( uniformManager <- renderPass.uniformList ) {
        if( uniformManager.ontologyMember.isDefined ) {
          sVarDescriptions = sVarDescriptions ::: uniformManager.ontologyMember.get :: Nil
        }
      }
    }
    sVarDescriptions
  }

  def getValueForSVarDescription[T]( sVarDescription : ConvertibleTrait[T]) : T = {
    var value : Option[T] = None
    for( renderPass <- this.renderPasses ) {
      for( uniformManager <- renderPass.uniformList ) {
        if( uniformManager.ontologyMember.isDefined && uniformManager.ontologyMember.get == sVarDescription ) {
          value = Some( uniformManager.ontologyMember.get.defaultValue().asInstanceOf[T] )
        }
      }
    }
    value.get

  }

}

class RenderPass( val name : String ) extends UniformListContaining[RenderPass] {

  private var shader = List[String]()
  var images : Map[String,String] = Map()
  private var shaderProgram : Option[ShaderProgram] = None
  var uniformList : List[UniformManager[_,RenderPass]] = List()

  def describedByShaders( shader : List[String] ) : RenderPass  = {
    this.shader = shader
    this
  }

  def provideImage( file : String ) : TextureMapper = {
    new TextureMapper( file, this )
  }

  class TextureMapper( val file : String, val renderPass : RenderPass ) {
    def as( targetName : String ) : RenderPass = {
      images = images + (file -> targetName )
      renderPass
    }

  }

  def where( name : String ) = {
    new UniformNameHolder( name, this )
  }

  def getShaderProgram : ShaderProgram = {
    if( shaderProgram.isEmpty ) {
      val shaderFiles = for( file <- shader ) yield new File( file )
      val sp = new ShaderProgram( shaderFiles: _* )
      shaderProgram = Some( sp )
    }
    this.shaderProgram.get
  }

}

object RenderPass {
  def apply( name : String ) : RenderPass = {
    new RenderPass( name )
  }
}

object ShaderEffect {

  def apply( name : String ) : ShaderEffect = {
    new ShaderEffect( name )
  }

  //def with( passName : String ) : ShaderEffect = {
  //   new ShaderEffect
  //}

}