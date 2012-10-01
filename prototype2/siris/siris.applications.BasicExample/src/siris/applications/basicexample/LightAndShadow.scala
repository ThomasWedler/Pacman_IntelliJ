package siris.applications.basicexample

import siris.components.physics.jbullet.JBulletComponent
import siris.components.renderer.messages._
import siris.core.SIRISApplication
import objects._
import actors.Actor
import siris.core.entity.description.SequentialRealize
import siris.components.editor.{EditorConfiguration, Editor}
import siris.components.physics.{PhysBox, PhysicsConfiguration}
import simplex3d.math.floatm.{Mat3x4f, ConstMat4f, Vec3f, ConstVec3f}
import simplex3d.math.floatm.FloatMath._
import siris.components.naming.NameIt
import siris.components.physics.ImplicitEitherConversion._
import siris.core.helper.{SchedulerAdjustment, TimeMeasurement}
import javax.media.opengl.GLProfile
import siris.core.svaractor.{SVarActorImpl, SVarActorLW}
import simplex3d.math.floatm.renamed._
import siris.components.worldinterface.{CreationMessage, WorldInterface}
import siris.ontology.{EntityDescription, types => gt}
import siris.components.renderer.jvr._
import siris.components.renderer.createparameter._
import java.io.File
import swing.{ScrollPane, MainFrame, FileChooser}
import javax.swing.filechooser.FileFilter
import java.awt.{Dimension, Component, Color}
import javax.swing.{JTextField, JLabel, JPanel}
import org.jfree.chart.{ChartPanel, ChartFactory}
import org.jfree.data.xy.XYDataset
import org.jfree.data.time.{Second, Millisecond, TimeSeriesCollection, TimeSeries}
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.axis.DateAxis

object LightAndShadow {

  val portsDir = (new scala.tools.nsc.CompileSocket()).portsDir

  def pollPort(): Int = portsDir.list.toList match {
      case Nil      => -1
      case x :: xs  => try x.name.toInt finally xs foreach (_.delete())
  }

  def main( String : Array[String] ) {
//    val z = 1.05
//    val e = 600.0
//    println("Einzahlung pro Jahr = " + e + " - Zins = " + z)
//    for(i <- 1 to 80) println(i + ". Jahr -> " + (e * getZ(z,i)).formatted("%.2f") + "€ (Eingezahlt " + (i.toDouble*e).formatted("%.2f") + "€)")

//    val cc = new scala.tools.nsc.CompileSocket()
//    println(cc.portsDir)
//    cc.portsDir.list.toList match {
//      case Nil      => scala.sys.process.Process((new File("scala/current/bin/scala")).getAbsolutePath + " scala.tools.nsc.CompileServer -v").run()
//      case x :: xs  => println("Scala compilation deamon is already running.")
//    }
//    val pp = pollPort()
//    println(pp)
//    val scalaRunable = if(JVMTools.isWindows) new File("scala/current/bin/scala.bat") else new File("scala/current/bin/scala")
//    if(pp < 0) scala.sys.process.Process(scalaRunable.getAbsolutePath + " scala.tools.nsc.CompileServer -v").run()

    //sys.exit()
    //println((new File("scala/current/bin/scala")).getAbsolutePath + " scala.tools.nsc.CompileServer -v")
    //scala.sys.process.Process((new File("scala/current/bin/scala")).getAbsolutePath + " scala.tools.nsc.CompileServer -v").run()

    val fc = new FileChooser()
    fc.title = "Select a Collada File"
    fc.multiSelectionEnabled = false
    fc.fileFilter = new FileFilter {
      def accept(f: File) =
        f.isDirectory || f.getName.toLowerCase.endsWith(".dae")

      def getDescription = "Collada Files"
    }

    fc.fileSelectionMode = FileChooser.SelectionMode.FilesOnly

    fc.controlButtonsAreShown = false

    val textfield = fc.peer.getComponents.map(c => flat(c)).flatten.toList.find(_.isInstanceOf[JTextField])
    textfield.collect{case tf => tf.getParent().getParent().remove(tf.getParent())}



    if(fc.showDialog(null, "Load") == FileChooser.Result.Approve) {
      fileToLoad = Some(fc.selectedFile)
    } else fileToLoad = Some(new File("fishtank/data/fish/cfish.DAE"))
    println(fileToLoad)





    start()
  }


  private def flat(c: Component, res: List[Component] = Nil): List[Component] = {
    c match {
      case jp: JPanel => jp.getComponents.map(comp => flat(comp, res)).flatten.toList
      case comp => comp :: res
    }
  }



  var fileToLoad: Option[File] = None

  def getZ(z: Double, y: Int) = {
    var res = 0.0
    for(i <- 1 to y) {
      res += math.pow(z,i.toDouble)
    }
    res
  }

  protected def start() {
    GLProfile.initSingleton(true)
    SchedulerAdjustment.adjustTheScheduler()
    appActor.start()
  }

  private val appActor = new SVarActorLW with TimeMeasurement {
    self =>

    private case class ScheduleShutdownIn(millis: Long)

    protected def initialize() {
    }

    protected def createComponents() {
      println("creating components")
      // create components
      //val physics  = new JBulletComponent('physics)
      val renderer = new JVRConnector('renderer)
      // register components at the WorldInterface
      WorldInterface.registerComponent(renderer)
      //WorldInterface.registerComponent(physics)
      // send configs
      renderer ! ConfigureRenderer( Actor.self, BasicDisplayConfiguration(800, 600), true, EffectsConfiguration( "high","none" ) )
      renderer ! RenderNextFrame( Actor.self )
      //renderer ! SetAmbientColor(Actor.self, new de.bht.jvr.util.Color(.9f,.9f,.9f))
      //renderer ! SubscribeForRenderSteps(self)
      //PhysicsConfiguration (ConstVec3f(0,-9.81f,0)).deliver()

      WorldInterface.registerComponent(new Editor('editor))
      EditorConfiguration(appName = "MasterControlProgram").deliver()
      // register for exit on close:
      exitOnClose(renderer, shutdownApp _)
      // start all registered components
      WorldInterface.handleRegisteredComponents(_.values.foreach(_.start()))

      WorldInterface.registerForCreationOf('user :: Nil)
      //renderer ! SetAmbientColor(Actor.self, Color.white)
    }

    addHandler[CreationMessage] {case msg =>

      msg match {
        case CreationMessage('user :: Nil, ent) =>
          ent.get(gt.ViewPlatform).get.set(ConstMat4f(Mat3x4f.rotateY(radians(-30)).translate(Vec3f(-10,0,0))))
          println("Cam set")
        case _ => println("Wrong ent")
      }

    }


    //}

    protected def exitOnClose( renderer : JVRConnector, shutdownMethod : Function0[Unit] ) {
      val self = SVarActorImpl.self
      renderer ! NotifyOnClose( self )
      self.addHandler[JVRRenderWindowClosed] {
        msg => shutdownMethod()
      }
    }

    protected def shutdownApp() {
      WorldInterface.getRegisteredComponents.values.foreach( _.shutdown() )
      WorldInterface.shutdown()
      super.shutdown()
    }

    protected def createEntities() {
      println("creating entities")


      val objManip: Option[List[ElementManipulator]] =
        Some(
          new NormalMaterial(
            "mesh1",
            "fishtank/data/models/rock_4/images/texture0.jpg",
            "fishtank/data/models/rock_4/images/texture0.jpg",
            5) ::
//          new NormalMaterial(
//            "mesh2",
//            "fishtank/data/models/rock/images/texture0.jpg",
//            "fishtank/data/models/rock/images/texture0.jpg",
//            5) ::
//          new NormalMaterial(
//            "mesh3",
//            "fishtank/data/models/rock/images/texture0.jpg",
//            "fishtank/data/models/rock/images/texture0.jpg",
//            5) ::
          Nil
        )


      fileToLoad.collect{ case file =>
        val objDesc = EntityDescription(
            ShapeFromFile(
              transformation = ConstMat4f(Mat3x4f.translate(Vec3f(0,0,-15))),   //ReadFromElseWhere,
              scale          = ConstMat4f(Mat3x4f.scale(0.3f)),
              //manipulatorList = objManip,
              file           = file.getAbsolutePath
            ),
            NameIt("Collada file '" + file.getName + "'")
          )

        objDesc.realize(e => {})
      }



      val manip: Option[List[ElementManipulator]] =
        Some(new NormalMaterial( "Plane001", "fishtank/data/images/floor.jpg", "fishtank/data/images/floor-normals.jpg", 40 ) :: Nil)

      val manip2: Option[List[ElementManipulator]] =
        Some(
          new ParallaxMaterial(
            "Plane001",
            "fishtank/data/images/floor.jpg",
            "fishtank/data/images/floor-displacement.jpg",
            "fishtank/data/images/floor-normals.jpg",
            0.01f,
            -0.00f,
            40)
          :: Nil
        )

//      EntityDescription(
//        ShapeFromFile(
//          transformation = ConstMat4f(Mat3x4f.translate(Vec3f(0, 0, -20))), //ReadFromElseWhere,
//          scale = ConstMat4f(Mat3x4f.scale(10f)),
//          manipulatorList = manip2,
//          file =  "fishtank/data/models/1x1-xy-square-stones.dae"
//        ),
//        NameIt("wall")
//      ).realize(e => {})



      val lightDesc =   EntityDescription(
          SpotLight(
            name           = "light",
            transformation = ConstMat4(Mat3x4f.translate(Vec3f(0,0,0))),
            diffuseColor   = new Color(0.5f, 0.6f, 0.5f),
            specularColor  = new Color(0.5f, 0.6f, 0.5f)
          ),
          NameIt("light")
        )

      lightDesc.realize(e => {})



  //    Table("the table", Vec3f(3f, 1f, 2f), Vec3f(0f, -1.5f, -7f)).realize( e => println("created new table") )
  //    Light("the light", Vec3f(-4f, 8f, -7f), 270f, -25f, 0f).realize( e => println("created new light"))
  //    Ball ("the ball", 0.2f,  Vec3f(0f, 1f, -7f)).realize( e => println("created new ball") )
  //
  //    SequentialRealize( Table("the table", Vec3f(3f, 1f, 2f), Vec3f(0f, -1.5f, -7f)).desc ).
  //      whereResultIsProcessedBy( e => println("created new table") ).
  //      thenRealize( Light("the light", Vec3f(-4f, 8f, -7f), 270f, -25f, 0f).desc ).
  //      whereResultIsProcessedBy( e => println("created new light") ).
  //      thenRealize( Ball ("the ball", 0.2f,  Vec3f(0f, 1f, -7f)).desc ).
  //      whereResultIsProcessedBy(  e => println("created new ball") ).
  //      execute
    }

    protected def finishConfiguration() {
      println("application is running")
    }

    override def startUp() {
      SchedulerAdjustment.adjustTheScheduler()
      createComponents()
      createEntities()
      finishConfiguration()
    }

    addHandler[ScheduleShutdownIn]{ case msg =>
      scheduleExecution(msg.millis) {
        shutdown()
        // TODO: Exit should not be necessary. Check why actors do not exit properly.
        System.exit(0)
      }
    }
  }
}