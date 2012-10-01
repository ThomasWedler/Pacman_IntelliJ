package siris.components.sound;

import org.lwjgl.openal.AL10;
import org.lwjgl.test.applet.*;
import org.lwjgl.util.WaveData;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OpenALWave{
// static properties
  /** ArrayList of waves to be created. */
  private static HashMap<String, OpenALWave> genWaves = new HashMap<String, OpenALWave>();
  private static IntBuffer buffers = null;

// wave properties
  /** The file of the wave file. */
   private File file;
  /** The id or name of the wave file. */
  int id;

  /**
   * Load a wave file. Don't use this methode directly. Use sound.OpenALWave.create(String path) instead
   * to avoid multiple loading of one of the same wave file.
   *
   * @param path the file name of the wave file
   */
  private OpenALWave(String path){
    file = new File(path);
    if(!file.exists()) {
        //System.err.println("File \"" + this.file.getAbsolutePath() + "\" doesn't exist. Check the file path." );
    }
    else {
    // add to list to load wave later
    genWaves.put(path, this);
    }
  }

  /**
   * Use this method to load wave files.
   *
   * @param path path the file name of the wave file
   * @return the wave object
   */
  public static OpenALWave create(String path){
    Object obj = genWaves.get(path);
    if(obj != null)
      return (OpenALWave) obj;
    else
      return new OpenALWave(path);
  }

    /**
     * Use this method to get the corresponding wave file.
     * @param path path the file name of the wave file
     * @return the wave object, null if wave file hasn't been created yet
     */
  public static OpenALWave get(String path){
    return genWaves.get(path);
  }

  /**
   * Generates all loaded waves.
   * Must be called before waves can be used with OpenAL.
   */
  public static void alGenWaves(){
    // create buffer
    buffers = OpenALUtils.createIntBuffer(genWaves.size());
    AL10.alGenBuffers(buffers);
    OpenALUtils.checkError();

    Iterator it = genWaves.entrySet().iterator();

    int i = 0;
    while(it.hasNext()){
        OpenALWave openALWave = (OpenALWave) (((Map.Entry) it.next()).getValue());

        //load wave data
        WaveData wavefile = null;
        try {
            URL url = openALWave.file.toURI().toURL();
            //System.out.println("Loading wave " + url);
            wavefile = WaveData.create(url);

            openALWave.id = buffers.get(i);

            //copy to buffers
            AL10.alBufferData(openALWave.id, wavefile.format, wavefile.data, wavefile.samplerate);

            OpenALUtils.checkError();

            //unload file again
            wavefile.dispose();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            //System.err.println("Error while loading .wav file: " + openALWave.file.getPath() );
            e.printStackTrace();
        }

        i++;
    }
  }

  /**
   * Deletes all generatet buffers.
   */
  public static void alDeleteWaves(){
    if(buffers != null){
      AL10.alDeleteBuffers(buffers);
      buffers = null;
    }
  }

  public String toString(){
    return "File name: \"" + file.getPath() + "\", ID: " + id;
  }
}
