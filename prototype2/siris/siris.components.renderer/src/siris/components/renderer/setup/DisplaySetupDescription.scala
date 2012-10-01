package siris.components.renderer.setup

import simplex3d.math.floatm.ConstMat4f

/**
 * This enumeration describes different link types between different logical displays.
 *
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 * date: 30.07.2010
 */

object LinkType extends Enumeration {

  /**
   * This values describes that two display belong together and shoudl be rendererd in one window in anaglyph mode.
   */
  val AnaglyphStereo = Value( "AnaglyphStereo" )

  val FrameSequential = Value( "FrameSequential" )

  // TODO: Document. I dont' know much about splitter.
  val SplitterLink = Value( "SplitterLink" )

  /**
   * This value describes that the displays are single displays.
   */
  val SingleDisplay = Value( "SingleDisplay" )
}

/**
 * This enumeration contains the possible eyes of a user.
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 */
object Eye extends Enumeration {
  /**
   * The left eye.
   */
  val LeftEye = Value( "LeftEye" )

  /**
   * The right eye.
   */
  val RightEye = Value( "RightEye" )
}

/**
 * This class describes a camera. It can also be interpreted as user.
 *
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 *
 * @param camId The id of the camera/user.
 * @param eye The eye that is drawn on the screen.
 */
class CamDesc( val camId : Int, val eye : Eye.Value )


/**
 * This class describes on display.
 *
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 *
 * @param resolution A optional resolution of the display. None means fullscreen.
 * @param size The width and height of the screen in meters.
 * @param transformation The physical transformation of the screen relative to the view platform (like center of the CAVE).
 */
class DisplayDesc( val resolution: Option[(Int, Int)], val size: (Double, Double), val transformation: ConstMat4f, val view: CamDesc )

/**
 * This class respresents a hardware display device.
 *
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 *
 * @param hardwareHandle A optional hardware handle. The meansings are (display, screen, channel). If's not set it depends on the OS where the window is opened.
 * @param displayDescs The displays behind this hardware handle.
 * @param linkType A description how the displays belongs together.
 */
class DisplayDevice( val hardwareHandle: Option[(Int, Int, Int)], val displayDescs : List[DisplayDesc], val linkType : LinkType.Value = LinkType.SingleDisplay )
{
  def this( hardwareHandle: Option[(Int, Int, Int)],firstDpy : DisplayDesc, linkType : LinkType.Value ) = this( hardwareHandle, firstDpy :: Nil, linkType )
}

/**
 * This class represents a display device group. All displays devices in the same group gets redrawn at the same time.
 *
 * @author Dennis Wiebusch
 * @author Stephan Rehfeld
 *
 * @param dpys A set of display.
 *
 */
class DisplayDeviceGroup( var dpys : Set[DisplayDevice] ) {

  /**
   * This method adds a display device to this display device group.
   *
   * @param dev The display device to add.
   */
  def addDevice( dev : DisplayDevice ) {
    dpys += dev
  }
}

/**
 * This class represents a display setup. I can be interpreted by a renderer connection to setup up windows
 * on everey display that shows the scene.
 */
class DisplaySetupDesc {

  /**
   * The device groups of this display setup.
   */
  var deviceGroups = Map[Int, DisplayDeviceGroup]()

  /**
   * This methods adds a display device to a display device group.
   *
   * @param device The display device.
   * @param groupId The id of the display device group the display device belongs to.
   * @return the display setup desc itself
   */
  def addDevice( dev : DisplayDevice , groupId : Int ) : DisplaySetupDesc = {
    val updated = deviceGroups.getOrElse(groupId, new DisplayDeviceGroup(Set[DisplayDevice]()))
    updated.addDevice(dev)
    deviceGroups = deviceGroups.updated(groupId, updated)
    this
  }
}


// Some notes below:

//Possible displays:
//HeadTracked/NonHeadTracked: SingleDisplay, AnaglyphStereo, ShutterStereo, PolFilterStereo
//DynamicDisplays, HeadMountedDisplays (Overlap < 100%), NonPlanarDisplays, HardwareSplitterDisplay
//TiledDisplays(EdgeBlending), MultiUserStereo, ???HolographicDisplays (Single pixels with depth information)

//Abstraction for non planar displays necessary
//Filter description for edge blending