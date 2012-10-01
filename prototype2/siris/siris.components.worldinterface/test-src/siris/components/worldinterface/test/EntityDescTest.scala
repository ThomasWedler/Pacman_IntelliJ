package siris.components.worldinterface.test

import org.scalatest.Spec
import org.scalatest.matchers.ShouldMatchers
import scala.concurrent.SyncVar
import actors.Actor
import siris.core.svaractor.{Shutdown, SVarActorImpl, SVar, SVarActorHW}
import siris.core.entity.Entity
import siris.core.component._
import siris.core.entity.description._
import siris.core.entity.component.Removability
import simplex3d.math.floatm.renamed._
import siris.core.entity.typeconversion.ConvertibleTrait
import simplex3d.math.floatm.Vec3f
import siris.ontology.{EntityDescription, Symbols, types}

/* author: dwiebusch
 * date: 06.09.2010
 */

class ExampleComponent(name : Semantics) extends SVarActorHW with Component{
  def componentName = Symbol("EntityDescTest:" + name.toSymbol.name)
  def componentType = name

  protected def removeFromLocalRep(e: Entity)     {}
  protected def configure(params: SValList) {}

  override protected def getAdditionalProvidings( aspect : EntityAspect ) =
    if (name == Symbols.physics)
      Set()
    else
      Set(types.Velocity, types.Color)

  override def newEntityConfigComplete(e: Entity with Removability, aspect: EntityAspect) {
    println("entityConfigComplete in " + name.toSymbol.name)
  }

  override def getInitialValues(toProvide: Set[ConvertibleTrait[_]], aspect: EntityAspect, e: Entity, given: SValList) = {
    println("requestInitialValues in " + name.toSymbol.name + " for " + toProvide)
    if (name == Symbols.physics )  toProvide.foldLeft(new SValList){ (set, elem) => elem match {
      case types.Transformation => set += types.Transformation(Mat4(Mat3x4.scale(0.1f)))
      case types.Health => set += types.Health(given.firstValueFor(types.Velocity).x)
      case types.Lives => set += types.Lives(given.firstValueFor(types.Velocity).y.intValue)
    }}
    else if (name == Symbols.graphics) toProvide.foldLeft(new SValList){ (set, elem) => elem match {
      case types.Velocity => set += types.Velocity(given.firstValueFor(types.Transformation).apply(1).xyz)
      case types.AngularDamping => set += types.AngularDamping(1f)
      case types.Transformation => set += types.Transformation(Mat4(Mat3x4.scale(2f)))
      case types.Color => set += types.Color(java.awt.Color.gray)
      case _ => set
    }}
    else new SValList
  }

  override def getDependencies(aspect: EntityAspect) = {
    println("getDependencies in " + name.toSymbol.name)
    val dep = if (name == Symbols.physics )
      Set(Dependencies(Providing(types.Health, types.Lives), Requiring(types.Velocity)))
    else
      Set(Dependencies(Providing(types.Velocity), Requiring(types.Transformation)))
    super.getDependencies(aspect) ++ dep
  }
}


case class GraphicsTestAspect(v : Vec3, d : Float, t : Mat4) extends Aspect(Symbols.graphics, Symbols.box, Nil){
  def getFeatures     = Set(types.Transformation, types.Health, types.AngularDamping)
  def getCreateParams = addCVars{ types.Velocity(v) and types.AngularDamping(d) and types.Transformation(t) }
  def getProvidings   = getCreateParams.map(_.typedSemantics).toSet
}

case class PhysicsTestAspect(h : Float, l : Int, t : Mat4) extends Aspect(Symbols.physics, Symbols.box, Nil){
  def getFeatures     = Set(types.Transformation, types.Health, types.Lives)
  def getCreateParams = addCVars{ types.Health(h) and types.Lives(l) and types.Transformation(t) }
  def getProvidings   = getCreateParams.map(_.typedSemantics).toSet
}

class NewEntityDescTest extends Spec with ShouldMatchers {
  val physics = new ExampleComponent(Symbols.physics)
  val graphics = new ExampleComponent(Symbols.graphics)
  val readActor = SVarActorImpl.actor{
    SVarActorImpl.self.addHandler[Read[_]]{
      case Read(svar, toSet) => svar.get( toSet.set( _ ) )
    }
  }

  physics.start()
  graphics.start()
  readActor.start()

  describe("New Entity Description Test:"){
    it("Creating Entity for a single component"){
      val e = EntityDescription(
        GraphicsTestAspect(Vec3f.Zero, 1f, Mat4.Zero) where(types.Transformation isOwned),
        PhysicsTestAspect(1f, 1, Mat4.Identity)       where(types.Transformation isProvided)
      ).realize( )
      println(e.getAllSVarNames)
      val x = new SyncVar[Mat4]
      readActor ! Read(e.get(types.Transformation).get, x)
      println(x.get)
    }
    it("Should end some time"){
      physics.shutdown()
      graphics.shutdown()
      readActor.shutdown()
    }
  }
}

case class Read[T]( svar: SVar[T], toSet : SyncVar[T] )

class EntityDescTest extends Spec with ShouldMatchers {
  val physics = new ExampleComponent(Symbols.physics)
  val graphics = new ExampleComponent(Symbols.graphics)

  val testTransform1 = types.Transformation createdBy (new Matrix(Matrix.float))
  val testTransform2 = testTransform1 createdBy (new RowMajorMatrix(Matrix.float))
  val testTransform3 = testTransform1 createdBy (new ColumnMajorMatrix(Matrix.taolf))

  physics.start()
  graphics.start()



  val readActor = SVarActorImpl.actor{
    SVarActorImpl.self.addHandler[Read[_]]{
      case Read(svar, toSet) => svar.get( toSet.set( _ ) )
    }
  }

  def read[T]( svar : SVar[T] ) : T = {
    val lock = new SyncVar[T]()
    readActor ! Read(svar, lock)
    lock.get
  }

  def checkMats( ms : Matrix[Float]*) {
    for (m <- ms)
      for ( i <- 0 to 3; j <- 0 to 3)
        if (m(i, j) != j + 1f)
          fail("transform failed")
    assert(true)
  }

  val ToRowMajorConverter = new MatrixConverter(testTransform3, testTransform2) {
    override def canConvert(from: ConvertibleTrait[_], to: ConvertibleTrait[_]) : Boolean ={
      convertType = from.typeinfo == testTransform3.typeinfo
      true
    }
  }

  readActor.start()

  println(testTransform2.defaultValue())
  println("  ")
  println(testTransform3.defaultValue())

  describe("Entity Description"){
    it("should work for isRequired and isProvided (no converters involved)") {
      val y = EntityDescription(
        EntityAspect(Symbols.physics, null, testTransform3 isProvided),
        EntityAspect(Symbols.graphics, null, testTransform3 isRequired)
      )
      val x = read(y.realize().get(testTransform3).get)
      checkMats(x)
      Thread.sleep(100)
    }
    it("should work for requiredAs and providedAs (no converters involved)") {
      Thread.sleep(100)
      val x = read(EntityDescription(
        EntityAspect(Symbols.physics, null, testTransform3 providedAs testTransform2),
        EntityAspect(Symbols.graphics, null, testTransform2 requiredAs testTransform3)
      ).realize().get(testTransform2).get)
      checkMats(x)
      Thread.sleep(100)
    }
    it("should work for isRequired and isProvided using converter") {
      Thread.sleep(100)
      val x = read(EntityDescription(
        EntityAspect(Symbols.physics, null, testTransform2 providedAs testTransform2),
        EntityAspect(Symbols.graphics, null, testTransform2 requiredAs testTransform3 using ToRowMajorConverter)
      ).realize().get(testTransform2).get)
      checkMats(x)
      Thread.sleep(100)
    }
    it("should work ;)"){
      Thread.sleep(100)
      val newEntity = EntityDescription(
        EntityAspect(Symbols.graphics, null, testTransform2 isRequired),
        EntityAspect(Symbols.physics, null, testTransform3 providedAs testTransform2)
      ).realize()
      val transformSVar1 = newEntity.get(testTransform3, ToRowMajorConverter).get
      checkMats( read( transformSVar1 ) )//, read( transformSVar2 ))
    }
    it("should end sometimes"){
      physics.shutdown()
      graphics.shutdown()
      readActor ! Shutdown( Actor.self )
      assert(true)
    }
  }
}
