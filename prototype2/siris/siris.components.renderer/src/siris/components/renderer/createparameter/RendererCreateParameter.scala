package siris.components.renderer.createparameter

import simplex3d.math.floatm.renamed.{ConstMat4, Mat3x4}
import siris.ontology.Symbols
import siris.ontology.types._
import java.awt.Color
import siris.core.entity.Entity
import siris.core.entity.typeconversion.ConvertibleTrait
import siris.core.entity.description.{SVal, Aspect, Semantics, SValSeq}

/**
 * @author Stephan Rehfeld
 */



class ReadFromElseWhere

object ReadFromElseWhere extends ReadFromElseWhere


package object convert {
  implicit def constMat4ToRightEither( transformation : ConstMat4 ) : Right[ReadFromElseWhere,ConstMat4] = Right( transformation )
  implicit def color4ToRightEither( color : Color ) : Right[ReadFromElseWhere,Color] = Right( color )
  implicit def float4ToRightEither( float : Float ) : Right[ReadFromElseWhere,Float] = Right( float )
  implicit def boolean4ToRightEither( boolean : Boolean ) : Right[ReadFromElseWhere,Boolean] = Right( boolean )
  implicit def readFromElseWhereToLeftEither( readFromElseWhere : ReadFromElseWhere ) : Left[ReadFromElseWhere,ReadFromElseWhere] = Left( ReadFromElseWhere )
}

abstract class RendererAspect( aspectType : SVal[Semantics], targets : List[Symbol] = Nil )
  extends Aspect( Symbols.graphics, aspectType, targets ) {
  def getProvidings =
    getFeatures
}


case class AnimatedObject( name: String,
                           subElement : Option[String] = None,
                           parentElement : Option[Entity] = None,
                           transformation : Either[ReadFromElseWhere,ConstMat4]  = Right( ConstMat4( Mat3x4.Identity ) ),
                           scale : ConstMat4 = ConstMat4( Mat3x4.Identity )
                        ) extends RendererAspect( Symbols.mesh ) {

  require( name != null, "The parameter 'name' must not be 'null'!" )
  require( subElement != null, "The parameter ' subElement' must not be 'null'!")
  require( parentElement != null, "The parameter ' subElement' must not be 'null'!")
  require( transformation != null, "The parameter ' subElement' must not be 'null'!")

  override def getCreateParams = {
    var cVars = new SValSeq
    cVars = cVars and Name( name )
    if( subElement.isDefined ) cVars = cVars and SubElement( subElement.get )
    if( parentElement.isDefined ) cVars = cVars and ParentElement( parentElement.get )
    if( transformation.isRight ) cVars = cVars and Transformation( transformation.right.get )
    cVars = cVars and Scale( scale )
    cVars = cVars and Texture(None)
    addCVars( cVars )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features + Transformation
    features = features + siris.ontology.types.Mesh
    features
  }

  override def getProvidings : Set[ConvertibleTrait[_]] = {
    var providings = Set[ConvertibleTrait[_]]()
    if( transformation.isRight ) providings = providings +  Transformation
    providings
  }



}


/**
 * This class represents the create param for a new spot light.
 *
 * @param name The name of the light withing the internal scene graph. Mandatory parameter.
 * @param transformation The transformation of the light. Mandatory parameter.
 * @param diffuseColor The diffuse color of the light. Default color is white.
 * @param specularColor The specular color of the light. Default color is white.
 * @param constantAttenuation The constant attenutation of the light. Default value is 1.0.
 * @param linearAttenuation The linear attenutation of the light. Default value is 0.0.
 * @param quadraticAttenuation The quadratic attenutation of the light. Default value is 0.0.
 * @param spotCutOff The spot cut off angle of the light. Default value is 30.
 * @param spotExponent The spot exponent of the light. Default value is 1.
 * @param shadowBias Default value is 0.25.
 */
case class SpotLight( name: String,
                      transformation : Either[ReadFromElseWhere,ConstMat4] = Right( ConstMat4( Mat3x4.Identity ) ),
                      parentElement : Option[ Entity ] = None,
                      diffuseColor : Either[ReadFromElseWhere,Color] = Right( Color.WHITE ),
                      specularColor : Either[ReadFromElseWhere,Color] = Right( Color.WHITE ),
                      constantAttenuation : Either[ReadFromElseWhere,Float] = Right( 1.0f ),
                      linearAttenuation : Either[ReadFromElseWhere,Float] = Right( 0.0f ),
                      quadraticAttenuation : Either[ReadFromElseWhere,Float] = Right( 0.0f ),
                      spotCutOff : Either[ReadFromElseWhere,Float] = Right( 30.0f ),
                      spotExponent : Either[ReadFromElseWhere,Float] = Right( 1.0f ),
                      castShadow : Either[ReadFromElseWhere,Boolean] = Right( true ),
                      shadowBias : Either[ReadFromElseWhere,Float] = Right( 0.25f ) ) extends RendererAspect( Symbols.spotLight ) {

  require( name != null, "The parameter 'name' must not be 'null'!" )
  require( transformation != null, "The parameter 'transformation' must not be 'null'!" )
  require( (transformation.isLeft && transformation.left.get != null) || (transformation.isRight && transformation.right.get != null), "Neither left or right value of parameter 'transformation' must not be 'null'!" )
  require( parentElement != null, "The parameter 'parentElement' must not be 'null'!" )
  require( parentElement.isEmpty || parentElement.get != null, "The parameter 'parentElement' must not be 'null'!" )
  require( diffuseColor != null, "The parameter 'diffuseColor' must not be 'null'!" )
  require( (diffuseColor.isLeft && diffuseColor.left.get != null) || (diffuseColor.isRight && diffuseColor.right.get != null), "Neither left or right value of parameter 'diffuseColor' must not be 'null'!" )
  require( specularColor != null, "The parameter 'specularColor' must not be 'null'!" )
  require( (specularColor.isLeft && specularColor.left.get != null) || (specularColor.isRight && specularColor.right.get != null), "Neither left or right value of parameter 'specularColor' must not be 'null'!" )
  require( constantAttenuation != null, "The parameter 'constantAttenuation' must not be 'null'!" )
  require( (constantAttenuation.isLeft && constantAttenuation.left.get != null) || (constantAttenuation.isRight), "Neither left or right value of parameter 'constantAttenuation' must not be 'null'!" )
  require( linearAttenuation != null, "The parameter 'linearAttenuation' must not be 'null'!" )
  require( (linearAttenuation.isLeft && linearAttenuation.left.get != null) || (linearAttenuation.isRight), "Neither left or right value of parameter 'linearAttenuation' must not be 'null'!" )
  require( quadraticAttenuation != null, "The parameter 'quadraticAttenuation' must not be 'null'!" )
  require( (quadraticAttenuation.isLeft && quadraticAttenuation.left.get != null) || (quadraticAttenuation.isRight), "Neither left or right value of parameter 'quadraticAttenuation' must not be 'null'!" )
  require( spotCutOff != null, "The parameter 'spotCutOff' must not be 'null'!" )
  require( (spotCutOff.isLeft && spotCutOff.left.get != null) || (spotCutOff.isRight), "Neither left or right value of parameter 'spotCutOff' must not be 'null'!" )
  require( spotExponent != null, "The parameter 'spotExponent' must not be 'null'!" )
  require( (spotExponent.isLeft && spotExponent.left.get != null) || (spotExponent.isRight), "Neither left or right value of parameter 'spotExponent' must not be 'null'!" )
  require( castShadow != null, "The parameter 'castShadow' must not be 'null'!" )
  require( (castShadow.isLeft && castShadow.left.get != null) || (castShadow.isRight), "Neither left or right value of parameter 'castShadow' must not be 'null'!" )
  require( shadowBias != null, "The parameter 'shadowBias' must not be 'null'!" )
  require( (shadowBias.isLeft && shadowBias.left.get != null) || (shadowBias.isRight), "Neither left or right value of parameter 'shadowBias' must not be 'null'!" )

  override def getCreateParams = {
    var cVars = new SValSeq
    cVars = cVars and Name( name )
    if( transformation.isRight ) cVars = cVars and Transformation( transformation.right.get )
    if( constantAttenuation.isRight ) cVars = cVars and ConstantAttenuation( constantAttenuation.right.get )
    if( linearAttenuation.isRight ) cVars = cVars and LinearAttenuation( linearAttenuation.right.get )
    if( quadraticAttenuation.isRight ) cVars = cVars and QuadraticAttenuation( quadraticAttenuation.right.get )
    if( spotCutOff.isRight ) cVars = cVars and SpotCutOff( spotCutOff.right.get )
    if( spotExponent.isRight ) cVars = cVars and SpotExponent( spotExponent.right.get )
    if( castShadow.isRight ) cVars = cVars and CastShadow( castShadow.right.get )
    if( shadowBias.isRight ) cVars = cVars and ShadowBias( shadowBias.right.get )
    if( diffuseColor.isRight ) cVars = cVars and DiffuseColor( diffuseColor.right.get )
    if( specularColor.isRight ) cVars = cVars and SpecularColor( specularColor.right.get )
    if( parentElement.isDefined ) cVars = cVars and ParentElement( parentElement.get )
    addCVars( cVars )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features +  Transformation
    features = features +  ConstantAttenuation
    features = features +  LinearAttenuation
    features = features +  QuadraticAttenuation
    features = features +  SpotCutOff
    features = features +  SpotExponent
    features = features +  CastShadow
    features = features +  ShadowBias
    features = features +  DiffuseColor
    features = features +  SpecularColor
    features
  }

  override def getProvidings : Set[ConvertibleTrait[_]] = {
    var providings = Set[ConvertibleTrait[_]]()
    if( transformation.isRight ) providings = providings +  Transformation
    if( constantAttenuation.isRight ) providings = providings +  ConstantAttenuation
    if( linearAttenuation.isRight ) providings = providings +  LinearAttenuation
    if( quadraticAttenuation.isRight ) providings = providings +  QuadraticAttenuation
    if( spotCutOff.isRight ) providings = providings +  SpotCutOff
    if( spotExponent.isRight ) providings = providings +  SpotExponent
    if( castShadow.isRight ) providings = providings +  CastShadow
    if( castShadow.isRight ) providings = providings +  ShadowBias
    if( diffuseColor.isRight ) providings = providings +  DiffuseColor
    if( specularColor.isRight ) providings = providings +  SpecularColor
    providings
  }

}

/**
 * This class represents the create param for a new point light.
 *
 * @param name The name of the light withing the internal scene graph. Mandatory parameter.
 * @param transformation The transformation of the light. Mandatory parameter.
 * @param diffuseColor The diffuse color of the light. Default color is white.
 * @param specularColor The specular color of the light. Default color is white.
 * @param constantAttenuation The constant attenutation of the light. Default value is 1.0.
 * @param linearAttenuation The linear attenutation of the light. Default value is 0.0.
 * @param quadraticAttenuation The quadratic attenutation of the light. Default value is 0.0.
 */
case class PointLight( name: String,
                       transformation : Either[ReadFromElseWhere,ConstMat4] = Right( ConstMat4( Mat3x4.Identity ) ),
                       parentElement : Option[ Entity ] = None,
                       diffuseColor : Either[ReadFromElseWhere,Color] = Right( Color.WHITE ),
                       specularColor : Either[ReadFromElseWhere,Color] = Right( Color.WHITE ),
                       constantAttenuation : Either[ReadFromElseWhere,Float] = Right( 1.0f ),
                       linearAttenuation : Either[ReadFromElseWhere,Float] = Right( 0.0f ),
                       quadraticAttenuation : Either[ReadFromElseWhere,Float] = Right( 0.0f )
                     ) extends RendererAspect( Symbols.pointLight ) {

  require( name != null, "The parameter 'name' must not be 'null'!" )
  require( transformation != null, "The parameter 'transformation' must not be 'null'!" )
  require( (transformation.isLeft && transformation.left.get != null) || (transformation.isRight && transformation.right.get != null), "Neither left or right value of parameter 'transformation' must not be 'null'!" )
  require( parentElement != null, "The parameter 'parentElement' must not be 'null'!" )
  require( parentElement.isEmpty || parentElement.get != null, "The parameter 'parentElement' must not be 'null'!" )
  require( diffuseColor != null, "The parameter 'diffuseColor' must not be 'null'!" )
  require( (diffuseColor.isLeft && diffuseColor.left.get != null) || (diffuseColor.isRight && diffuseColor.right.get != null), "Neither left or right value of parameter 'diffuseColor' must not be 'null'!" )
  require( specularColor != null, "The parameter 'specularColor' must not be 'null'!" )
  require( (specularColor.isLeft && specularColor.left.get != null) || (specularColor.isRight && specularColor.right.get != null), "Neither left or right value of parameter 'specularColor' must not be 'null'!" )
  require( constantAttenuation != null, "The parameter 'constantAttenuation' must not be 'null'!" )
  require( (constantAttenuation.isLeft && constantAttenuation.left.get != null) || (constantAttenuation.isRight), "Neither left or right value of parameter 'constantAttenuation' must not be 'null'!" )
  require( linearAttenuation != null, "The parameter 'linearAttenuation' must not be 'null'!" )
  require( (linearAttenuation.isLeft && linearAttenuation.left.get != null) || (linearAttenuation.isRight), "Neither left or right value of parameter 'linearAttenuation' must not be 'null'!" )
  require( quadraticAttenuation != null, "The parameter 'quadraticAttenuation' must not be 'null'!" )
  require( (quadraticAttenuation.isLeft && quadraticAttenuation.left.get != null) || (quadraticAttenuation.isRight), "Neither left or right value of parameter 'quadraticAttenuation' must not be 'null'!" )


  override def getCreateParams = {
    var cVars = new SValSeq
    cVars = cVars and Name( name )
    if( transformation.isRight ) cVars = cVars and Transformation( transformation.right.get )
    if( constantAttenuation.isRight ) cVars = cVars and ConstantAttenuation( constantAttenuation.right.get )
    if( linearAttenuation.isRight ) cVars = cVars and LinearAttenuation( linearAttenuation.right.get )
    if( quadraticAttenuation.isRight ) cVars = cVars and QuadraticAttenuation( quadraticAttenuation.right.get )
    if( diffuseColor.isRight ) cVars = cVars and DiffuseColor( diffuseColor.right.get )
    if( specularColor.isRight ) cVars = cVars and SpecularColor( specularColor.right.get )
    if( parentElement.isDefined ) cVars = cVars and ParentElement( parentElement.get )
    addCVars( cVars )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features +  Transformation
    features = features +  ConstantAttenuation
    features = features +  LinearAttenuation
    features = features +  QuadraticAttenuation
    features = features +  DiffuseColor
    features = features +  SpecularColor
    features
  }

  override def getProvidings : Set[ConvertibleTrait[_]] = {
    var providings = Set[ConvertibleTrait[_]]()
    if( transformation.isRight ) providings = providings +  Transformation
    if( constantAttenuation.isRight ) providings = providings +  ConstantAttenuation
    if( linearAttenuation.isRight ) providings = providings +  LinearAttenuation
    if( quadraticAttenuation.isRight ) providings = providings +  QuadraticAttenuation
    if( diffuseColor.isRight ) providings = providings +  DiffuseColor
    if( specularColor.isRight ) providings = providings +  SpecularColor
    providings
  }

}

case class SkyBox( name: String,
                   frontTexture : String,
                   backTexture : String,
                   leftTexture : String,
                   rightTexture : String,
                   topTexture : String,
                   bottomTexture : String
                  ) extends RendererAspect( Symbols.skyBox ) {
  require( name != null, "The parameter 'name' must not be 'null'!" )
  require( frontTexture != null, "The parameter 'frontTexture' must not be 'null'!" )
  require( backTexture != null, "The parameter 'backTexture' must not be 'null'!" )
  require( leftTexture != null, "The parameter 'leftTexture' must not be 'null'!" )
  require( rightTexture != null, "The parameter 'rightTexture' must not be 'null'!" )
  require( topTexture != null, "The parameter 'topTexture' must not be 'null'!" )
  require( bottomTexture != null, "The parameter 'bottomTexture' must not be 'null'!" )

  override def getCreateParams = addCVars {
      Name( name ) and
      FrontTexture( frontTexture ) and
      BackTexture( backTexture ) and
      LeftTexture( leftTexture ) and
      RightTexture( rightTexture ) and
      UpTexture( topTexture ) and
      DownTexture( bottomTexture )

  }

  override def getFeatures = Set()

}

case class ShapeFromFile( file: String,
                          subElement : Option[String] = None,
                          parentElement : Option[Entity] = None,
                          transformation : Either[ReadFromElseWhere,ConstMat4]  = Right( ConstMat4( Mat3x4.Identity ) ),
                          scale : ConstMat4 = ConstMat4( Mat3x4.Identity ),
                          manipulatorList : Option[List[ElementManipulator]] = None
                        ) extends RendererAspect( Symbols.shapeFromFile ) {

  require( file != null, "The parameter 'file' must not be 'null'!" )
  require( subElement != null, "The parameter ' subElement' must not be 'null'!")

  override def getCreateParams = {
    var cVars = new SValSeq
    cVars = cVars and ColladaFile( file )
    if( subElement.isDefined ) cVars = cVars and SubElement( subElement.get )
    if( transformation.isRight ) cVars = cVars and Transformation( transformation.right.get )
    cVars = cVars and Scale( scale )
    if( manipulatorList.isDefined ) cVars = cVars and ManipulatorList(manipulatorList.get)
    addCVars( cVars )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features +  Transformation
    features
  }

  override def getProvidings : Set[ConvertibleTrait[_]] = {
    var providings = Set[ConvertibleTrait[_]]()
    if( transformation.isRight ) providings = providings +  Transformation
    providings
  }

}

case class Fog( nearClip: Float,
                farClip : Float,
                skyBoxFog: Float
              ) extends RendererAspect( Symbols.fog ) {


  override def getCreateParams = addCVars {
      NearClip( nearClip ) and
      FarClip( farClip ) and
      SkyBoxFog( skyBoxFog )
  }

  override def getFeatures = getCreateParams.map(_.typedSemantics).toSet

}

case class Interface(

                    ) extends RendererAspect( Symbols.interface ) {

  override def getCreateParams = addCVars {
    Seq()
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features +  siris.ontology.types.Health
    features = features +  siris.ontology.types.Mana
    features = features +  siris.ontology.types.Lives
    features
  }
}

case class Mirror( name: String,
                   file : String,
                   transformation : Either[ReadFromElseWhere,ConstMat4] = Right( ConstMat4( Mat3x4.Identity ) )
                 ) extends RendererAspect( Symbols.mirror ) {

  require( name != null, "The parameter 'name' must not be 'null'!" )
  require( file != null, "The parameter 'file' must not be 'null'!" )
  require( transformation != null, "The parameter 'transformation' must not be 'null'!" )
  require( (transformation.isLeft && transformation.left.get != null) || (transformation.isRight && transformation.right.get != null), "Neither left or right value of parameter 'transformation' must not be 'null'!" )

  override def getCreateParams = {
    var cVars = new SValSeq
    cVars = cVars and Name( name )
    cVars = cVars and ColladaFile( file )
    if( transformation.isRight ) cVars = cVars and Transformation( transformation.right.get )
    addCVars( cVars )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features +  Transformation
    features
  }

  override def getProvidings : Set[ConvertibleTrait[_]] = {
    var providings = Set[ConvertibleTrait[_]]()
    if( transformation.isRight ) providings = providings +  Transformation
    providings
  }

}

case class Water( name: String,
                   file : String,
                   transformation : Either[ReadFromElseWhere,ConstMat4] = Right( ConstMat4( Mat3x4.Identity ) ),
                   waveScale : Either[ReadFromElseWhere,Float]
                 ) extends RendererAspect( Symbols.water ) {

  require( name != null, "The parameter 'name' must not be 'null'!" )
  require( file != null, "The parameter 'file' must not be 'null'!" )
  require( transformation != null, "The parameter 'transformation' must not be 'null'!" )
  require( (transformation.isLeft && transformation.left.get != null) || (transformation.isRight && transformation.right.get != null), "Neither left or right value of parameter 'transformation' must not be 'null'!" )
  require( waveScale != null, "The parameter 'waveScale' must not be 'null'!" )
  require( (waveScale.isLeft && waveScale.left.get != null) || (waveScale.isRight), "Neither left or right value of parameter 'waveScale' must not be 'null'!" )


  override def getCreateParams = {
    var cVars = new SValSeq
    cVars = cVars and Name( name )
    cVars = cVars and File( file )
    if( transformation.isRight ) cVars = cVars and Transformation( transformation.right.get )
    if( waveScale.isRight ) cVars = cVars and WaveScale( waveScale.right.get )
    addCVars( cVars )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features +  Transformation
    features = features +  WaveScale
    features
  }

  override def getProvidings : Set[ConvertibleTrait[_]] = {
    var providings = Set[ConvertibleTrait[_]]()
    if( transformation.isRight ) providings = providings +  Transformation
    if( waveScale.isRight ) providings = providings +  WaveScale
    providings
  }

}

case class ExistingNode(
                         subElement: String,
                         scale : ConstMat4 = ConstMat4( Mat3x4.Identity )
                 ) extends RendererAspect( Symbols.existingNode ) {

  override def getCreateParams = addCVars {
      SubElement( subElement ) and Scale( scale )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features +  Transformation
    features
  }

  override def getProvidings : Set[ConvertibleTrait[_]] = {
    var providings = Set[ConvertibleTrait[_]]()
    providings = providings +  Transformation
    providings
  }

}

case class VRUser(  viewPlatform : ConstMat4 = ConstMat4( Mat3x4.Identity ),
                    headTransform : ConstMat4 = ConstMat4( Mat3x4.Identity )

                   ) extends RendererAspect( Symbols.user ) {
  override def getCreateParams = addCVars {
    ViewPlatform( viewPlatform ) and HeadTransform( headTransform )
  }

  override def getFeatures : Set[ConvertibleTrait[_]] = {
    var features = Set[ConvertibleTrait[_]]()
    features = features +  ViewPlatform
    features = features +  HeadTransform
    features
  }

}

case class DepthOfFieldEffect (
                         intensity: Float = 0.0f
                 ) extends RendererAspect( Symbols.depthOfFieldEffect ) {

  override def getCreateParams = addCVars {
      Seq( Intensity( intensity ) )
  }

  override def getFeatures = getCreateParams.map(_.typedSemantics).toSet
}

case class BloomEffect (
                         threshhold: Float = 0.5f,
                         factor: Float = 3.0f
                 ) extends RendererAspect( Symbols.bloomEffect ) {

  override def getCreateParams = addCVars {
    Threshold( threshhold ) and  Factor( factor )
  }

  override def getFeatures = getCreateParams.map(_.typedSemantics).toSet
}

case class SaturationEffect (
                         saturation: Float = 1.0f
                 ) extends RendererAspect( Symbols.saturationEffect ) {

  override def getCreateParams = addCVars {
      Seq( Saturation( saturation ) )
  }

  override def getFeatures = getCreateParams.map(_.typedSemantics).toSet
}

//ExistingNode