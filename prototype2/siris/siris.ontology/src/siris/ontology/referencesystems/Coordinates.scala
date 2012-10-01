package siris.ontology.referencesystems

import java.io.File
import java.util.UUID
import simplex3d.math.floatm.renamed._
import simplex3d.math.floatm.FloatMath.{inverse, radians}
import siris.core.svaractor.{SVarActorLW, SVar}
import siris.core.helper.SchemaAwareXML
import siris.ontology.Symbols
import actors.Actor
import xml.Node
import siris.core.entity.description.{Semantics, SVal}

/**
 * User: dwiebusch
 * Date: 16.05.11
 * Time: 11:24
 */

object CoordinateSystemConverter{
  /**
   * creates an wrapper for an already registered system
   * @param name the name of the registered system
   * @return an wrapper for the system
   */
  def apply[U](name : SVal[Semantics]) : CoordinateSystemConverter[U] =
    new CoordinateSystemConverter[U](name)
}

class CoordinateSystemConverter[U] protected( name : SVal[Semantics], transform : ConstMat4 ){
  //register this system
  Coordinates.setSystem(name, transform)
  //some shortcuts to the function calls
  private val fromRef : U => U = Coordinates.convert(_, Symbols.origin, name)
  private val toRef   : U => U = Coordinates.convert(_, name, Symbols.origin)

  /**
   * Constructor for internal use (used by the companion object), to create wrappers
   * @param the name of the aspect type to be wrapped
   */
  protected def this( name : SVal[Semantics] ) =
    this(name, Coordinates.getSystem(name).getOrElse(throw new NoMappingException(name.toString)) )

  /**
   * Constructor to load system from file
   * @param name the semantics denoting the component for which this system will be registered
   * @param file the file to be loaded
   * @param nodeName the name of the xml-node parenting the nodes scale, translate[X-Z], rotate[X-Z]
   */
  def this( name : SVal[Semantics], file : File, nodeName : String ) =
    this(name, Coordinates.loadFile(file, nodeName) )

  /**
   * the alternative ctor, ensuring that no scale is entered into the rotation part of the transform
   * @param name the semantics denoting the component for which this system will be registered
   * @param rotation the rotation part of the coordinate transform
   * @param scale the scaling part of the coordinate Transform
   * @param translation the translation part of the coordinate transform
   */
  def this( name : SVal[Semantics],
            rotation : Mat3x4  = Mat3x4.scale(1f),
            scale    : Mat3x4  = Mat3x4.scale(1f),
            translation : Vec3 = Vec3(0, 0, 0) ) =
    this(name, ConstMat4(rotation.translate(scale * Vec4(translation, 1))))

  /**
   * transforms local coordinates into the reference coordinate system
   * @param inLocalCoords the coordinates to be transformed
   * @return the transformed coordinates
   */
  def toRefCoords( inLocalCoords : U ) : U =
    toRef(inLocalCoords)

  /**
   * transforms reference coordinates into the local coordinate system
   * @param inRefCoords the coordinates to be transformed
   * @return the transformed coordinates
   */
  def fromRefCoords( inRefCoords : U ) : U =
    fromRef(inRefCoords)

  /**
   * transforms local coordinates into the given coordinate system
   * @param inLocalCoords the coordinates to be transformed
   * @param thatSystem the identifier denoting the other coordinate system
   * @return the transformed coordinates
   */
  def convertTo( inLocalCoords : U, thatSystem : SVal[Semantics] ) : U =
    Coordinates.convert(inLocalCoords, name, thatSystem)
}

/**
 * holder for coordinate systems, providing conversion methods
 */
protected object Coordinates extends ReferenceSystem[ConstMat4]{
  private var names = Map[SVal[Semantics], UUID](Symbols.origin -> UUID.randomUUID)
  private var viewPlatformObserver : Option[Actor] = None

  private case class End()
  private case class Update( vpf : SVar[ConstMat4] )
  private class ViewPlatformObserver extends SVarActorLW{
    private var svar : Option[SVar[ConstMat4]] = None
    private def ignore = svar.collect{ case sv => sv.ignore()  }
    private def observe( vpf : SVar[ConstMat4] ) : Option[SVar[ConstMat4]] = {
      ignore
      vpf.get    ( value => setSystem( Symbols.viewPlatform, value ) )
      vpf.observe( value => setSystem( Symbols.viewPlatform, value ) )
      Some(vpf)
    }

    addHandler[Update]{ msg => svar = observe(msg.vpf) }
    addHandler[End]{    msg => ignore                  }
  }

  setSystem(Symbols.origin, ConstMat4(Mat4.Identity))
  setSystem(Symbols.viewPlatform, ConstMat4(Mat4.Identity))

  def loadCoordinateSetup(name : SVal[Semantics], filename : String, nodeName : String ) =
    setSystem( name, loadFile(new File(filename), nodeName) )

  def getSystem( name : SVal[Semantics] ) : Option[ConstMat4] =
    getMapping(names.getOrElse(name, throw NoMappingException(name.toString)))

  def setSystem( name : SVal[Semantics], rotation : Mat3x4, scale : Float, translation : Vec3 ) : UUID =
    setSystem(name, ConstMat4(scale * rotation.translate(translation)))

  def setSystem( name : SVal[Semantics], transform : ConstMat4) : UUID = synchronized {
    names = names.updated(name, names.getOrElse(name, UUID.randomUUID))
    addMapping(names(name), transform, inverse(transform))
  }

  def setViewPlatform( vpf : SVar[ConstMat4] ) {
    if (viewPlatformObserver.isEmpty)
      viewPlatformObserver = Some(new ViewPlatformObserver().start())
    viewPlatformObserver.get ! Update(vpf)
  }

  def shutdown =
    viewPlatformObserver.collect{ case obs : SVarActorLW => obs.shutdown() }

  //for ease of use
  def convert[U]( toConvert : U, inSystem : SVal[Semantics], outSystem : UUID ) : U =
    convert(toConvert, names.getOrElse(inSystem, throw NoMappingException(inSystem.toString)), outSystem)

  def convert[U]( toConvert : U, inSystem : UUID, outSystem : SVal[Semantics] ) : U =
    convert(toConvert, inSystem, names.getOrElse(outSystem, throw NoMappingException(outSystem.toString)))

  def convert[U]( toConvert : U, inSystem : SVal[Semantics], outSystem : SVal[Semantics] ) : U =
    convert(toConvert,
      names.getOrElse(inSystem, throw NoMappingException(inSystem.toString)),
      names.getOrElse(outSystem, throw NoMappingException(outSystem.toString))
    )

  def convert[U]( toConvert : U, inSystem : UUID, outSystem : UUID ) : U =
    convertTo[U, U](toConvert,
      getInverse(inSystem ).getOrElse(throw NoMappingException("id " + inSystem)),
      getMapping(outSystem).getOrElse(throw NoMappingException("id " + outSystem))
    )

  def loadFile(descFile : File, nodeName : String) : ConstMat4 =
    readTransformFromXML((SchemaAwareXML.loadFile(descFile) \ nodeName).head)

  protected def convertTo[U, V](toConvert: U, inSystem: ConstMat4, outSystem: ConstMat4) : V = (toConvert match {
    case value : inMat4x3 => outSystem * inSystem * value
    case value : inMat4x2 => outSystem * inSystem * value
    case value : inMat4   => outSystem * inSystem * value
    case value : inVec4   => outSystem * inSystem * value
    case _                => throw NoConversionPossibleException(toConvert)
  }).asInstanceOf[V]

  private def readTransformFromXML(n: Node) : ConstMat4 = {
    val translateX = (n \ ("translateX")).text.toFloat
    val translateY = (n \ ("translateY")).text.toFloat
    val translateZ = (n \ ("translateZ")).text.toFloat

    val rotateX = (n \ ("rotateX")).text.toFloat
    val rotateY = (n \ ("rotateY")).text.toFloat
    val rotateZ = (n \ ("rotateZ")).text.toFloat

    val scale = (n \ ("scale")).text.toFloat

    ConstMat4( Mat3x4.
      rotateX(radians(rotateX)).rotateY(radians(rotateY)).rotateZ(radians(rotateZ)).
      scale(scale).translate( Vec3(translateX, translateY, translateZ) )
    )
  }
}
