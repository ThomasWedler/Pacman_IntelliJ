package siris.core.helper

import siris.components.renderer.messages.FinishedFrame
import org.jfree.data.time.{TimeSeriesCollection, TimeSeries, Millisecond}
import org.jfree.chart.axis.DateAxis
import org.jfree.chart.{ChartPanel, ChartFactory}
import org.jfree.chart.plot.XYPlot
import scala.swing.MainFrame
import java.awt.Dimension

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 8/22/12
 * Time: 1:52 PM
 */
trait WorkloadMonitor extends TimeMeasurement {

  private val wm_size = 60
  private val wm_stepTimes = new Array[Long](wm_size)
  private var wm_c = 0

  def wm_name = ""

  /**
   * Start an actor that sends the calling actor a WakeUpMessage after the given period of time.
   */
  override protected def requestWakeUpCall(in: WorkloadMonitor.this.Period) {
    super.requestWakeUpCall(in)
    wm_stepTimes(wm_c) = System.nanoTime()
    wm_c += 1
    if(wm_c >= wm_size) {
      wm_c = 0
      printFps()
    }
  }

  private def printFps() {
    val diff = (wm_stepTimes(wm_size-1) - wm_stepTimes(0)).toFloat / 1000000f
    val perFrameMean = diff / wm_size.toFloat
    var max = Float.MinValue
    var min = Float.MaxValue
    for(i <- 0 until (wm_size-1)) {
      val step = (wm_stepTimes(i+1)-wm_stepTimes(i)).toFloat / 1000000f
      if(step > max) max = step
      if(step < min) min = step
    }
    val now = new Millisecond()
    wm_means.add(now, perFrameMean)
    wm_mins.add(now, min)
    wm_maxes.add(now, max)
    wm_plot.collect{case p =>
      p.getDomainAxis match {
        case a: DateAxis =>
          a.setRange(new java.util.Date(System.currentTimeMillis()-10000L), new java.util.Date(System.currentTimeMillis()+1000L))
        case _ =>
      }
    }
    //println("FPS: " +  fps + " Max: " + max + " Min: " + min)
  }


 // def createChart() {
  private val wm_means = new TimeSeries("Mean")
  private val wm_maxes = new TimeSeries("Max")
  private val wm_mins = new TimeSeries("Min")



  private val wm_dataSet = new TimeSeriesCollection()
  wm_dataSet.addSeries(wm_means)
  wm_dataSet.addSeries(wm_maxes)
  wm_dataSet.addSeries(wm_mins)

  private val wm_chart = ChartFactory.createTimeSeriesChart(wm_name + "'s Frame Length", "System Time", "[millis]", wm_dataSet, true, true, false)


  private val wm_plot = wm_chart.getPlot match {
    case xyplot: XYPlot => Some(xyplot)
    case _ => None
  }

//  wm_plot.collect{case p =>
//    p.getRangeAxis match {
//      case ax => println(ax.getClass.getCanonicalName)
//    }
//  }

  private val wm_chartPanel = new ChartPanel(wm_chart)
  //panel.setFillZoomRectangle(true)
  //panel.setMouseWheelEnabled(true)

  private val wm_mf = new MainFrame {
    peer.setContentPane(wm_chartPanel)
    size = new Dimension(800, 600)
    minimumSize = new Dimension(800, 600)
  }
  wm_mf.visible = true

}
