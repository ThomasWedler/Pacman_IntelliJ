package siris.components.sound;

import org.lwjgl.openal.AL10;
import org.lwjgl.openal.OpenALException;

import java.io.File;
import java.nio.IntBuffer;

public class OpenALSource{
// static
  /** Maximal count of sources. */
  protected static final int MAX_SOURCES = 64;

  /** IntBuffer of all available sources. */
  private static IntBuffer sources;
  /** Array of the appendant OpenALSources. */
  private static OpenALSource[] openALSources;

  public SoundObject owner = null;


  /**
   * Set the reference distance, the rolloff factor and the maximal distance of the source.
   *
   * @param id the id of the source
   */
  private static void setSoundEnvironment(int id){
    AL10.alSourcef(id, AL10.AL_REFERENCE_DISTANCE, 100.0f);
    AL10.alSourcef(id, AL10.AL_ROLLOFF_FACTOR, 0.5f);
    AL10.alSourcef(id, AL10.AL_MAX_DISTANCE, 1000);
  }

  /**
   * Resets properties of the source.
   *
   * @param id the id of the source
   */
  public static void resetSource(int id){
    AL10.alSourcef(id, AL10.AL_GAIN, 1);
  }

  /**
   * Allocate all available sources.
   */
  public static void alGenSoureces(){
    // get all available sources
    for(int i = MAX_SOURCES; i > 0; i--){
      sources = OpenALUtils.createIntBuffer(i);
      AL10.alGenSources(sources);
      if(AL10.alGetError() == AL10.AL_NO_ERROR) break;
    }

    // create empty openALSources
    openALSources = new OpenALSource[sources.capacity()];
    for(int i = 0; i < openALSources.length; i++){
      openALSources[i] = new OpenALSource();
      setSoundEnvironment(sources.get(i));
      openALSources[i].winControl(sources.get(i));
    }

//    System.out.println("Jeah! I get " + sources.capacity() + " sources.");
  }

  /**
   * Frees all allocated sources.
   */
  public static void alDeleteSources(){
    // free all allocated sources
    try{ AL10.alDeleteSources(sources); }
    catch(OpenALException e) { }
}

// non static
  /** The source id (name). */
  private int id;
  /** Do I have the control over the source? Can I play some waves now? */
  private boolean hasControl = false;

  /**
   * Creates an openAL source.
   */
  public OpenALSource(){
  }

  /**
   * Return the id of the source.
   *
   * @return id of the source
   */
  public int getId(){
    return id;
  }

   public static OpenALSource getFreeSource() {
      // search for non playing source
    for(int i = 0; i < openALSources.length; i++){
      OpenALSource openALSource = openALSources[i];
      int state = openALSource.getPropi(AL10.AL_SOURCE_STATE);
     // System.out.println("state: " + state);
      if(state != AL10.AL_PLAYING && state != AL10.AL_PAUSED){
        // we've found an unused source
        int id = openALSource.getId();
        //openALSource.loseControl();
        if(openALSource.owner != null) openALSource.owner.setSourceChanged(true);
        resetSource(id);
        //this.winControl(id);
        openALSource.setPropf(AL10.AL_GAIN, 1);
        //System.out.println("use sound source " + i + " for source id " + id);
       // System.out.println(i + ": " + openALSource);
        return openALSource;
      }
    }
    return null;
   }

  /**
   * Try to get control over an unused source.
   *
   * @return true if we have now control over an source
   */
  public OpenALSource bind(){
     int state = this.getPropi(AL10.AL_SOURCE_STATE);
      /*System.out.println("state: " + state);*/
      if(state != AL10.AL_PLAYING && state != AL10.AL_PAUSED){
          return this;
      }
      return getFreeSource();

   /* if(hasControl) return true;*/

    // search for non playing source
/*    for(int i = 0; i < openALSources.length; i++){
      OpenALSource openALSource = openALSources[i];
      int state = openALSource.getPropi(AL10.AL_SOURCE_STATE);
      System.out.println("state: " + state);
      if(state != AL10.AL_PLAYING && state != AL10.AL_PAUSED){
        // we've found an unused source
        int id = openALSource.getId();
        openALSource.loseControl();
        resetSource(id);
        this.winControl(id);
        this.setPropf(AL10.AL_GAIN, 1);
        System.out.println("use sound source " + i + " for source id " + id);
        System.out.println(i + ": " + this);
        return true;
      }
    }

    System.out.println("no sound source available");
    return false;*/
  }

  /**
   * The openALSource get control over an source.
   *
   * @param id the new id (name)
   */
  private void winControl(int id){
    this.id = id;
    hasControl = true;
  }

  /**
   * The openALSource lose it's control over the source.
   */
  protected void loseControl(){
    hasControl = false;
  }

  /**
   * Returns true if the openALSource has the control over a source.
   *
   * @return true if the openALSource has the control over a source
   */
  public boolean hasControl(){
    return hasControl;
  }

  public int getPropi(int prop){
    return AL10.alGetSourcei(id, prop);
  }

  public float getPropf(int prop){
    return AL10.alGetSourcef(id, prop);
  }

  public void setPropi(int prop, int value){
    AL10.alSourcei(id, prop, value);
  }

  public void setPropf(int prop, float value){
    AL10.alSourcef(id, prop, value);
  }

  public void setProp3f(int prop, float value1, float value2, float value3){
    AL10.alSource3f(id, prop, value1, value2, value3);
  }

  public void setPos(float x, float y, float z){
    AL10.alSource3f(id, AL10.AL_POSITION, x, y, z);
  }

  /**
   * Start playing the wave file.
   *
   * @param openALWave the wave file.
   */
  public void play(OpenALWave openALWave){
    // set up source input
    try{ AL10.alSourcei(id, AL10.AL_BUFFER, openALWave.id);
        AL10.alSourcePlay(id);
       // System.out.println("play: " + id);
    }
    catch(OpenALException e) { }
  }

  public void pause(){
    try{ AL10.alSourcePause(id); /*System.out.println("pause: " + id);*/ }
    catch(OpenALException e) {}
  }

  public void stop(){
    try{ AL10.alSourceStop(id); /*System.out.println("stop: " + id); */}
    catch(OpenALException e) {}
  }

  public void rewind(){
    try{ AL10.alSourceRewind(id); }
    catch(OpenALException e) {}
  }

  public void dispose(){
  }

  public boolean isPlaying() {
    int state = this.getPropi(AL10.AL_SOURCE_STATE);
    if(AL10.AL_PLAYING == state) return true;
    else return false;
  }

  public boolean isPaused() {
    int state = this.getPropi(AL10.AL_SOURCE_STATE);
    if(AL10.AL_PAUSED == state) return true;
    else return false;
  }

  public String toString(){
    return "ID: " + id + ", HasControl: " + (hasControl ? "yes" : "no");
  }
}
