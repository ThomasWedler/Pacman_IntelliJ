/*
 * Created by IntelliJ IDEA.
 * User: anke
 * Date: 06.03.11
 * Time: 19:19
 */
package siris.components.sound;

import siris.core.entity.description.SValList

import org.lwjgl.openal.AL10

import scala.collection._
import siris.core.svaractor.SVar
import java.io.File
import siris.components.eventhandling.EventDescription
import simplex3d.math.floatm.{Mat4f, Vec3f}

class SoundEntity(var material : Symbol = 'None){
  //var onCollision: Option[SoundObject] = None
  var onCreation: Option[SoundObject] = None
  val onEvent =  mutable.Map[ EventDescription, SoundObject]()

  var svar: Option[SVar[Mat4f]] = None

  def setPositionSVar(svar : SVar[Mat4f]){

  }
}

/**
 * desrc
 * @param  hello
 */
class SoundObject (filePath: String, loop: Boolean = false){

  def this(c: SValList) = this(
    c.firstValueFor(OpenALComponent.soundObject.soundFile),
    c.getFirstValueForOrElse(OpenALComponent.soundObject.looping)(false)
  )

  val looping: Boolean = loop
  var sourceChanged = true

  def setSourceChanged( b : Boolean) { sourceChanged = b}

  private var oALWave: OpenALWave = null
  protected  var oALSource = OpenALSource.getFreeSource;

  init(filePath)

  def init(filePath: String) {
    val file = new File(filePath)
    if (!file.exists) sys.error("Error while loading .wav file: " + file.getPath)
    else {

      oALWave = OpenALWave.get(file.getPath)
      if (oALWave == null) {
        oALWave = OpenALWave.create(file.getPath)
        OpenALWave.alGenWaves()
      }

     /* if (looping) {
        if (oALSource.bind()) oALSource.setPropi(AL10.AL_LOOPING, AL10.AL_TRUE)
      }*/
    }
  }

  def play(e : SoundEntity*) {

    if(sourceChanged) {
      oALSource = oALSource.bind

      if(oALSource != null) {
          sourceChanged = false
          oALSource.owner = this
      }

    }
    if (oALSource != null) {
      if (looping) {
        //println("play " + oALWave.toString)
          if (!oALSource.isPlaying) {
            //println("play " + oALWave.toString + ", sourceId: " + oALSource.getId)
            oALSource.setPropi(AL10.AL_LOOPING, AL10.AL_TRUE)
            oALSource.play(oALWave)
          }

      }
      else {
        if (!oALSource.isPlaying) {
          //println("play " + oALWave.toString + ", sourceId: " + oALSource.getId)
          // println("play " + oALWave.toString)
          oALSource.setPropi(AL10.AL_LOOPING, AL10.AL_FALSE)
          oALSource.play(oALWave)
        }

      }
    }
  }

  def pause() {
    if(!sourceChanged) { oALSource.pause() }
  }

  def stop() {
        //println("stop " + oALWave.toString + ", sourceId: " + oALSource.getId)
    if(!sourceChanged) oALSource.stop()



  }

  def setPosition(pos : Vec3f) {
    oALSource.setPos(pos.x, pos.y, pos.z)
  }

  override def toString = "" + oALWave + ": -\\- sources are playing"
}

class SharedSoundObject(filePath: String, maxNoChannels: Int = 1, loop: Boolean = false)
  extends SoundObject(filePath, loop) {

  val maxNoOfChannels: Int = maxNoChannels

  val entities = mutable.Map[SoundEntity, Option[OpenALSource]]()
  val sources = mutable.Map[OpenALSource, Option[SoundEntity]]()

  def initSources(){
    for(i <- 0 until maxNoOfChannels) sources+=(new OpenALSource) -> None
  }


  def this(c: SValList) = this(
    c.firstValueFor(OpenALComponent.soundObject.soundFile),
    c.getFirstValueForOrElse(OpenALComponent.soundObject.shared)(1),
    c.getFirstValueForOrElse(OpenALComponent.soundObject.looping)(false)
  )

  private def assignSource(e1: SoundEntity, e2: SoundEntity): Boolean = {
    var bound = true
    entities.get(e1).collect {
      case sourceOption => {
        sourceOption.collect {
          case source =>
            bound = bindSource(source)
        }
      }
    }
    if(!bound) false
    entities.get(e2).collect {
      case sourceOption => {
        sourceOption.collect {
          case source =>
            bound = bindSource(source)
        }
      }
    }
    if(!bound) false

    if(assignSource(e1))  true
   // if(assignSource(e2))  true
    else false
  }


  private def assignSource(e: SoundEntity): Boolean = {
    entities.get(e) match {

      case None => {
        addEntity(e)
        assignSource(e)
      }

      case Some(sourceOption) =>
        (sourceOption) match {
        /*look for another free source*/
          case None => assignNextFreeSource(e)
          case Some(source) => bindSource(source)
        }
    }
  }

  override def play(e: SoundEntity*) {
    e.size match {
      case 1 => {
        if(assignSource(e.apply(0))) super.play()
      }
      case 2 => {
        if(assignSource(e.apply(0), e.apply(1))) super.play()
      }
    }


  }

  private def bindSource(source : OpenALSource): Boolean = {
     /*look if assigned source is free for play*/
   /* if (source.bind) {
      oALSource = source
      true
    } else false*/
    true
  }

  /**
   *  Look for another free source.
   */
  private def assignNextFreeSource(e: SoundEntity) : Boolean = {
     sources.find((sourceOptionOfEntityPair) => {
       val source = sourceOptionOfEntityPair._1
       sourceOptionOfEntityPair._2 match {
       /*nothing assigned. Assign it!*/
         case None =>
           entities.update(e, Some(source))
           sources.update(source, Some(e))
           oALSource = source
           true
         /*something assigned.*/
         case Some(entity) =>
         /* Look if it's free*/
        /*   if (source.bind) {
             /*change owner*/
             entities.update(e, Some(source))
             entities.update(entity, None)
             sources.update(source, Some(e))
             oALSource = source
             true
           }
           else
             false*/
           true
       }
     }).isDefined
  }

  def addEntity(e: SoundEntity) {
    entities += e -> None
  }

  def deleteEntity(e : SoundEntity){
      entities.get(e).collect{case sourceOption => sourceOption.collect{ case soundSource => sources.update(soundSource, None)}}
      entities.remove(e)
  }



}

class SimpleSoundObject{

}


