package siris.components.renderer.jvr

import de.bht.jvr.core.pipeline.Pipeline
import de.bht.jvr.core.{ShaderProgram, ShaderMaterial}
import java.io.File
import siris.ontology.SVarDescription
import siris.core.entity.Entity
import siris.core.svaractor.SVar
import de.bht.jvr.util.Color
import siris.core.entity.typeconversion.ConvertibleTrait
import de.bht.jvr.core.uniforms._
import simplex3d.math.floatm.Vec2f
import de.bht.jvr.math.Vector2


/**
 * Created by IntelliJ IDEA.
 * User: stephan_rehfeld
 * Date: 06.05.11
 * Time: 14:39
 * To change this template use File | Settings | File Templates.
 */

class PostProcessingEffect() extends UniformListContaining[PostProcessingEffect] {

  var nameOfEffect : Option[String] = None
  private var targetFBO : Option[String] = None
  private var ratio : Option[Float] = None
  private var clearTarget : Boolean = true
  private var shader : List[String] = List()
  private var nameForColorBuffer : Option[String] = None
  private var nameForDepthBuffer : Option[String] = None
  private var usedFrameBufferObjects : Map[String,String] = Map()
  private var shaderMaterial : Option[ShaderMaterial] = None
  private var deltaTName : Option[String] = None
  var uniformList : List[UniformManager[_,PostProcessingEffect]] = List()
  var images : Map[String,String] = Map()
  var isOverlay = false
  var createTargetFBO = true

  def provideDeltaTAs( deltaTName : String ) = {
    this.deltaTName = Some( deltaTName )
    this
  }

  def isAnOverlay : PostProcessingEffect = {
    this.isOverlay = true
    this
  }

  def describedByShaders( shader : List[String] ) : PostProcessingEffect  = {
    this.shader = shader
    this
  }

  def writesResultIn( name : String ) : PostProcessingEffect = {
    targetFBO = Some( name )
    createTargetFBO = true
    this
  }

  def writesResultInAlreadyCreated( name : String ) : PostProcessingEffect = {
    targetFBO = Some( name )
    createTargetFBO = false
    this
  }

  def clearTarget( clear : Boolean ) : PostProcessingEffect = {
    clearTarget = clear
    this
  }

  def withRatio( ratio : Float )  : PostProcessingEffect = {
    this.ratio = Some( ratio )
    this
  }

  def usingColorBufferAsName( name : String ) : PostProcessingEffect = {
    nameForColorBuffer = Some( name )
    this
  }

  def usingDepthBufferAsName( name : String )  : PostProcessingEffect = {
    nameForDepthBuffer = Some( name )
    this
  }

  def usingFrameBufferObjectWithName( name : String ) :  FrameBufferMapper = {
    new FrameBufferMapper( name, this )
  }

  def where( name : String ) = {
    new UniformNameHolder( name, this )
  }

  def provideImage( file : String ) : TextureMapper = {
    new TextureMapper( file, this )
  }

  def getShaderMaterial : ShaderMaterial = {
    if( !shaderMaterial.isDefined ) {
      val shaderFiles = for( file <- shader ) yield new File( file )
      val sm = new ShaderMaterial( nameOfEffect.get, new ShaderProgram( shaderFiles: _* ) )
      for( uniform <- uniformList ) {
        val uniformValue = uniform.value match {
          case v : Float =>
            new UniformFloat( v )
          case v : Int =>
            new UniformInt( v )
          case v : Boolean =>
            new UniformBool( v )
          case v : Vector2 =>
            new UniformVector2( v )
          case v : List[_] =>
            sm.setUniform( nameOfEffect.get, uniform.name + "_size", new UniformInt( v.size ) )
            if( v.isEmpty ) {
              new UniformVector2( new Vector2( 0.0f, 0.0f ) )
            } else {
              v.head match {
                case h : Vec2f => {
                  val jvrList = v.asInstanceOf[List[Vec2f]].map(s3dVec => {new Vector2(s3dVec.x, s3dVec.y)})
                  new UniformVector2( jvrList.toSeq.toArray : _* )
                }
              }
            }
        }
        sm.setUniform( nameOfEffect.get, uniform.name, uniformValue )
      }
      shaderMaterial = Some( sm )
    }
    shaderMaterial.get
  }

  def contributeToPipeline( pipeline : Pipeline, colorFBO : String, depthFBO : String ) : Boolean = {
    var switchFBO = true
    if( targetFBO.isDefined ) {
      if( createTargetFBO) pipeline.createFrameBufferObject( targetFBO.get, true, 1, this.ratio.getOrElse( 1.0f ), 4 )
      pipeline.switchFrameBufferObject( targetFBO.get )
      if( this.clearTarget ) pipeline.clearBuffers( true, true, new Color(0,0,0) )
      switchFBO = false
    }
    if( nameForColorBuffer.isDefined ) pipeline.bindColorBuffer( nameForColorBuffer.get, colorFBO, 0 )
    if( nameForDepthBuffer.isDefined ) pipeline.bindDepthBuffer( nameForDepthBuffer.get, depthFBO )
    for( (fbo,name) <- usedFrameBufferObjects ) pipeline.bindColorBuffer( name, fbo, 0 )

    val shaderMaterial = getShaderMaterial
    for( (image,name) <- images ) shaderMaterial.setTexture( nameOfEffect.get, name, ResourceManager.loadTexture( new File( image ) ) )

    pipeline.drawQuad( shaderMaterial, nameOfEffect.get )
    switchFBO
  }

  def bindShaderMaterialToEntity( entity : Entity ) {
    for( uniformManager <- this.uniformList ) {
      if( uniformManager.ontologyMember.isDefined )
        uniformManager.value match {
          case v : Float =>
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Float]].set(v)
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Float]].observe( ( v : Float ) => { getShaderMaterial.setUniform( nameOfEffect.get, uniformManager.name, new UniformFloat( v ) ) }  )
          case v : Int =>
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Int]].set(v)
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Int]].observe( ( v : Int ) => { getShaderMaterial.setUniform( nameOfEffect.get, uniformManager.name, new UniformInt( v ) ) }  )
          case v : Boolean =>
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Boolean]].set(v)
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Boolean]].observe( ( v : Boolean ) => {
              getShaderMaterial.setUniform( nameOfEffect.get, uniformManager.name, new UniformBool( v ) ) }  )
          case v : Vector2 =>
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Vector2]].set(v)
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[Vector2]].observe( ( v : Vector2 ) => { getShaderMaterial.setUniform( nameOfEffect.get, uniformManager.name, new UniformVector2( v ) ) }  )
          case v : List[_] =>
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[List[Vec2f]]].set(v.asInstanceOf[List[Vec2f]])
            entity.get( uniformManager.ontologyMember.get ).get.asInstanceOf[SVar[List[Vec2f]]].observe( ( list : List[Vec2f] ) => {
              getShaderMaterial.setUniform( nameOfEffect.get, uniformManager.name + "_size", new UniformInt( list.size ) )
              if( list.isEmpty ) {
                getShaderMaterial.setUniform( nameOfEffect.get, uniformManager.name, new UniformVector2( new Vector2( 0.0f, 0.0f ) ) )
              } else {
                val jvrList = list.map(s3dVec => {new Vector2(s3dVec.x, s3dVec.y)})
                getShaderMaterial.setUniform( nameOfEffect.get, uniformManager.name, new UniformVector2( jvrList.toArray : _* ) )
              }
            })
      }
    }
  }

  class FrameBufferMapper( val fboName : String, val ppe : PostProcessingEffect ) {
    def as( targetName : String ) : PostProcessingEffect = {
      usedFrameBufferObjects = usedFrameBufferObjects + (fboName -> targetName )
      ppe
    }

  }

  class TextureMapper( val file : String, val ppe : PostProcessingEffect ) {
    def as( targetName : String ) : PostProcessingEffect = {
      images = images + (file -> targetName )
      ppe
    }

  }

  def getSVarDescriptions : List[SVarDescription[_,_]] = {
    var sVarDescriptions = List[SVarDescription[_,_]]()
    for( uniformManager <- this.uniformList ) {
      if( uniformManager.ontologyMember.isDefined ) {
        sVarDescriptions = sVarDescriptions ::: uniformManager.ontologyMember.get :: Nil
      }
    }
    sVarDescriptions
  }

  def getValueForSVarDescription[T]( sVarDescription : ConvertibleTrait[T]) : T = {
    var value : Option[T] = None
    for( uniformManager <- this.uniformList ) {
      if( uniformManager.ontologyMember.isDefined && uniformManager.ontologyMember.get == sVarDescription ) {
        value = Some( uniformManager.ontologyMember.get.defaultValue().asInstanceOf[T] )
      }
    }
    value.get

  }

  def setCurrentDeltaT( deltaT : Float ) {
    if( this.deltaTName.isDefined ) {
      this.getShaderMaterial.setUniform( this.nameOfEffect.get, this.deltaTName.get, new UniformFloat( deltaT ) )
    }
  }
}

object PostProcessingEffect {

  def apply( nameOfEffect : String ) : PostProcessingEffect = {
    val e = new PostProcessingEffect
    e.nameOfEffect = Some( nameOfEffect )
    e
  }

}