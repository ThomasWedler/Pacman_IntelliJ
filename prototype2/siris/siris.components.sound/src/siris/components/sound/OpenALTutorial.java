/*
package siris.components.sound;
import org.lwjgl.openal.AL10;

*/
/**
 * Demonstrates the usage of OpenAL (jworms.openAL.*).
 *
 * usage: java tutorial.BigTextureTutorial
 *        (close Strg + C or wait 20 seconds)
 *//*

public class OpenALTutorial{
  // OpenAL sound source for explosion
  private OpenALSource footstepSoundSource = new OpenALSource();
  // OpenAL sound source for explosion
  private OpenALSource explosionSoundSource = new OpenALSource();

  // Sounds to play
  private OpenALWave explosion = null;
  private OpenALWave footSteps = null;

  public static void main(String[] arguments){
    new OpenALTutorial();
  }

  */
/**
   * Initialize OpenAL and play explosion and footsteps sound.
   *//*

  public OpenALTutorial(){
    // initialize OpenAL
    OpenALUtils.alInit();



    // load the sounds    s
    try{
      explosion = OpenALWave.create("../tutorial/Explosion.wav");
      footSteps = OpenALWave.create("tutorial/Footsteps.wav");
    } catch(Exception e){
      System.out.println("Failed starting OpenAL due to ");
      e.printStackTrace();
      System.exit(-1);
    }

    // generate all OpenAL sound sources
    OpenALWave.alGenWaves();

    // set OpenAL listener position
    OpenALListener.setPos(0, 0, 100);

    // play footsteps always left of the OpenAL listener
    if(footstepSoundSource.bind()){
      // right of the listener
      footstepSoundSource.setPos(-300, 0, 0);

      // play footsteps in loop
      footstepSoundSource.setPropi(AL10.AL_LOOPING, AL10.AL_TRUE);
      footstepSoundSource.play(footSteps);
    }

    // close Strg + C or wait 20 seconds
    int explosionX = 500;

    long startTime = System.currentTimeMillis();
    while(startTime + 20 * 1000 > System.currentTimeMillis()){

      // play an explosion every 2 seconds
      if(explosionSoundSource.bind()){
        explosionSoundSource.stop();

        // explosion sound moves from right to left side
        explosionSoundSource.setPos(explosionX, 0, 0);
        explosionSoundSource.play(explosion);
      }

      // move the explosion from right to left (and then again right)
      if(explosionX <= -500) explosionX = 500; else explosionX -= 250;

      // wait 2 seconds
      try{
       //Todo: Use TimeMeassurement Trait to schedule the following code in 2 sec
       } catch(InterruptedException e){ }
    }

    // dispose OpenAL
    OpenALUtils.alExit();
  }
}
*/
