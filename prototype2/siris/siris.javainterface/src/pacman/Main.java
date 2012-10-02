package pacman;

import siris.pacman.util.TilePosition;

import java.io.File;
import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: Thomas
 * Date: 01.10.12
 * Time: 23:36
 * To change this template use File | Settings | File Templates.
 */
public class Main {

    public static Game game;

    public static void main(String[] args) throws IOException, InterruptedException {
        game = new Game(new File("siris.javainterface/src/maps/testlevel.txt"));
    }

}
