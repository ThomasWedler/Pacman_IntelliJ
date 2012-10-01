package siris.components.renderer.setup

import simplex3d.math.floatm.renamed._
import simplex3d.math.floatm.FloatMath._
import siris.core.svaractor.SVar
import siris.ontology.{types => gt}
import simplex3d.math.floatm.ConstMat4f

/**
 * Created by IntelliJ IDEA.
 * User: stephan.rehfeld
 * Date: 04.10.2010
 * Time: 06:32:46
 * To change this template use File | Settings | File Templates.
 */

object DisplayDescriptions {

   val oneMeterInInches = 39.3700787
   val oneInchInMeters = 1.0/oneMeterInInches


   def ieeeVRSingleWindowDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val displayDesc = new DisplayDesc( Some((800,600)), (22.1, 12.4), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -14.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  def ieeeVRSingleFullscreenDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val displayDesc = new DisplayDesc( None, (22.1, 12.4), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -14.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  def ieeeVRStereoDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val displayDescLeft = new DisplayDesc( None, (22.1, 12.4), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -14.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )
    val displayDescRight = new DisplayDesc( None, (22.1, 12.4), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, 14.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDescLeft :: displayDescRight :: Nil, LinkType.FrameSequential )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  def ieeeVRAnaglyphDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val displayDescLeft = new DisplayDesc( Some((800,600)), (22.1, 12.4), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -14.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )
    val displayDescRight = new DisplayDesc( Some((800,600)), (22.1, 12.4), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -14.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDescLeft :: displayDescRight :: Nil, LinkType.AnaglyphStereo )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

   def bhtRenderServerConsoleDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val displayDesc = new DisplayDesc( Some((1920,1200)), (5.2, 3.31), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  def bhtRenderServerConsoleFullscreenDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val displayDesc = new DisplayDesc( None, (0.52, 0.331), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  def bhtRenderServerConsoleAnaglyphDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val leftDisplayDesc = new DisplayDesc( Some((1920,1200)), (0.52, 0.331), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )
    val rightDisplayDesc = new DisplayDesc( Some((1920,1200)), (0.52, 0.331), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, leftDisplayDesc :: rightDisplayDesc :: Nil, LinkType.AnaglyphStereo )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  def bhtHolodeckDisplayDescription : DisplaySetupDesc = {
    val displaySetupDesc = new DisplaySetupDesc

    val leftDisplayDesc = new DisplayDesc( None, (3.73, 2.33), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -1.17f ) ) ), new CamDesc( 0, Eye.LeftEye )  )
    val rightDisplayDesc = new DisplayDesc( None, (3.73, 2.33), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -1.17f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val leftDisplayDevice = new DisplayDevice( None, leftDisplayDesc :: Nil, LinkType.SingleDisplay )
    val rightDisplayDevice = new DisplayDevice( Some((0, 1, 0)), rightDisplayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( leftDisplayDevice, 0 )
    displaySetupDesc.addDevice( rightDisplayDevice, 0 )

    displaySetupDesc
  }

  def bhtHolodeckSingleDisplayDescription : DisplaySetupDesc = {
    val displaySetupDesc = new DisplaySetupDesc

    val leftDisplayDesc = new DisplayDesc( Some((1920,1200)), (37.3, 23.3), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 11.65f, -11.7f ) ) ), new CamDesc( 0, Eye.LeftEye )  )

    val leftDisplayDevice = new DisplayDevice( None, leftDisplayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( leftDisplayDevice, 0 )

    displaySetupDesc
  }

  def bhtMacBookStephanDisplayDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val displayDesc = new DisplayDesc( Some((1440,900)), (0.331,0.206), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  def bhtMacBookStephanDisplayAnaglyphDescription :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val leftDisplayDesc = new DisplayDesc( Some((1440,900)), (0.331,0.206), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )
    val rightDisplayDesc = new DisplayDesc( Some((1440,900)), (0.331,0.206), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, leftDisplayDesc :: rightDisplayDesc :: Nil, LinkType.AnaglyphStereo )

    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }


  def twoWindowDisplayDescription : DisplaySetupDesc = {
    val displaySetupDesc = new DisplaySetupDesc

    val leftDisplayDesc = new DisplayDesc( Some((640,480)), (0.331,0.206), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )
    val rightDisplayDesc = new DisplayDesc( Some((640,480)), (0.331,0.206), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val leftDisplayDevice = new DisplayDevice( Some((0,1,0)), leftDisplayDesc :: Nil, LinkType.SingleDisplay )
    val rightDisplayDevice = new DisplayDevice( Some((0,0,0)), rightDisplayDesc :: Nil, LinkType.SingleDisplay )

    displaySetupDesc.addDevice( leftDisplayDevice, 0 )
    displaySetupDesc.addDevice( rightDisplayDevice, 0 )

    displaySetupDesc
  }

  /**
   *
   * Creates a low quality display setup description that should fit for every desktop
   * computer or laptop.
   */
  def lqSingleWindow :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val widthInPx: Int = 800
    val heightinPx: Int = 600
    val dpi: Double = 96.0 * 0.5 // the factor 0.5 lets the user see "more sceene" in the window

    val ipd = 1.0/dpi
    val widthOnScreenInInches = ipd * widthInPx.toDouble
    val widthOnScreenInMeter = widthOnScreenInInches * oneInchInMeters


    val displayDesc = new DisplayDesc( Some((widthInPx,heightinPx)), (widthOnScreenInMeter,widthOnScreenInMeter * ( heightinPx.toDouble / widthInPx.toDouble)), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )
    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  /**
   *
   * Creates a display setup description that should fit for every desktop
   * computer or laptop.
   */
  def customSingleWindow(widthInPx: Int, heightinPx: Int) :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val dpi: Double = 96.0 * 0.5 // the factor 0.5 lets the user see "more sceene" in the window

    val ipd = 1.0/dpi
    val widthOnScreenInInches = ipd * widthInPx.toDouble
    val widthOnScreenInMeter = widthOnScreenInInches * oneInchInMeters


    val displayDesc = new DisplayDesc( Some((widthInPx,heightinPx)), (widthOnScreenInMeter,widthOnScreenInMeter * ( heightinPx.toDouble / widthInPx.toDouble)), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )
    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

    /**
   *
   * Creates a display setup description that should fit for every desktop
   * computer or laptop.
   */
  def customSingleWindowFixedBigViewPort(widthInPx: Int, heightinPx: Int) :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val dpi: Double = 96.0 * 0.5 // the factor 0.5 lets the user see "more sceene" in the window

    val ipd = 1.0/dpi
    val widthOnScreenInInches = ipd * 1920.0
    val widthOnScreenInMeter = widthOnScreenInInches * oneInchInMeters


    val displayDesc = new DisplayDesc( Some((widthInPx,heightinPx)), (widthOnScreenInMeter,widthOnScreenInMeter * ( 1080.0 / 1920.0)), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )
    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  //UNIT = dm !!!
  def ubtWindow(widthInPx: Int, heightinPx: Int) :DisplaySetupDesc = {
    val displaySetupDesc = new DisplaySetupDesc
    val displayDesc = new DisplayDesc( Some(widthInPx, heightinPx), (27.24, 16.3), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -23.45f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )
    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  //UNIT = dm !!!
  def ubtFullscreen: DisplaySetupDesc = {
    val displaySetupDesc = new DisplaySetupDesc
    val displayDesc = new DisplayDesc( None, (27.24, 16.3), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -23.45f ) ) ), new CamDesc( 0, Eye.RightEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: Nil, LinkType.SingleDisplay )
    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }

  def customAnaglyphWindowFixedBigViewPort(widthInPx: Int, heightinPx: Int) :DisplaySetupDesc ={
    val displaySetupDesc = new DisplaySetupDesc

    val dpi: Double = 96.0 * 0.5 // the factor 0.5 lets the user see "more sceene" in the window

    val ipd = 1.0/dpi
    val widthOnScreenInInches = ipd * 1920.0
    val widthOnScreenInMeter = widthOnScreenInInches * oneInchInMeters


    val displayDesc = new DisplayDesc( Some((widthInPx,heightinPx)), (widthOnScreenInMeter,widthOnScreenInMeter * ( 1080.0 / 1920.0)), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.RightEye )  )
    val displayDesc2 = new DisplayDesc( Some((widthInPx,heightinPx)), (widthOnScreenInMeter,widthOnScreenInMeter * ( 1080.0 / 1920.0)), ConstMat4( Mat3x4.translate( Vec3( 0.0f, 0.0f, -0.6f ) ) ), new CamDesc( 0, Eye.LeftEye )  )

    val displayDevice = new DisplayDevice( None, displayDesc :: displayDesc2 :: Nil, LinkType.AnaglyphStereo )
    displaySetupDesc.addDevice( displayDevice, 0 )

    displaySetupDesc
  }


//  abstract class Unit {
//    def toMeter
//    def fromMeter
//  }
//
//  object Units {
//
//    private class UnitImpl(toMeter: Float) extends Unit {
//      def fromMeter = 1.0f / fromMeter
//    }
//
//    private def createUnit(toMeter: Float): Unit = {new UnitImpl(toMeter)}
//
//    val Meter = createUnit(1f)
//    val Centimeter = createUnit(0.01f)
//  }
//  case class TransformWithUnits(transform: ConstMat4f, unit: Unit)

  def addTransformationProperagtion(source: SVar[gt.Transformation.dataType], sink: SVar[gt.Transformation.dataType], sourceToSinkTransform: ConstMat4f) {
    source.observe((srcTrans) => {println( srcTrans ); sink.set(inverse(sourceToSinkTransform) * srcTrans)})
  }
}