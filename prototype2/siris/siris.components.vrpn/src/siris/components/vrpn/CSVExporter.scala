package siris.components.vrpn

import devices.SimpleTarget
import siris.core.helper.TimeMeasurement
import siris.ontology.types.Transformation
import java.io.{FileWriter, BufferedWriter, File}
import siris.core.svaractor.{SVar, SVarActorHW}

/**
 * User: dwiebusch
 * Date: 08.06.12
 * Time: 16:19
 */

object CSVExporter {
  val MainActor = new SVarActorHW with TimeMeasurement{
    var svar : Option[SVar[Transformation.dataType]] = None
    addHandler[WakeUp]{ msg =>
      requestWakeUpCall(new Period(8))
      svar.collect{case sth => sth.get(x => writeCSV("1", x))}
    }

    override def startUp() {
      openFile("test.csv")
      SimpleTarget("Tracker0@localhost", Symbol("0")).realize{
        e => e.get(Transformation).collect{
          case t => svar = Some(t)
        }
      }
      requestWakeUpCall(new Period(0))
    }

    private var oStream : Option[BufferedWriter] = None

    def openFile(name : String){
      oStream = Some(new BufferedWriter(new FileWriter(new File(name))))
    }


    override def shutdown() {
      super.shutdown()
      oStream.collect{
        case stream =>
          stream.close()
          System.exit(0)
      }
    }

    def writeCSV(id : String, transform : Transformation.dataType){
      oStream.collect{
        case stream =>
          val toWrite = id + ";" + transform.toString() + "\n"
          stream.write(toWrite)
      }
    }
  }


  def main(args : Array[String]){
    val vrpn = new VRPNConnector()
    vrpn.start()
    MainActor.start()
    Thread.sleep(10000)
    vrpn.shutdown()
    MainActor.shutdown()
    println("shutdown")
  }
}
