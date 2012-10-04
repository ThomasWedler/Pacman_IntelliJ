package pacman;

import basic.*;
import siris.java.JavaInterface;
import siris.pacman.BasicPacman;
import siris.pacman.PacmanAI;
import siris.pacman.graph.JavaGraphWrapper;

import java.io.File;
import java.io.IOException;

public class Game {

    private MyLevel level;

    /* set mode to
       - "Normal" to start game
       - "TestSearch" to start graphical representation of used search-algorithm (@see basic.MyGraphSearch to specify method)
     */
    private String mode = "Normal";

    // Set pacmanAI to use for BasicPacman.startPacman
    private MyPacmanAI pacmanAI = new MyPacmanAI();

    // Set MyGraphSearch to use for BasicPacman.startPacman or testSearch
    private MyGraphSearch gs = new MyGraphSearch();

    /**
     * Start Pacman or testSearch
     * @param file Level to use for BasicPacman.startPacman or testSearch
     * @throws IOException if file not found/not readable
     * @throws InterruptedException
     */
    public Game(File file) throws IOException, InterruptedException {
        level = new MyLevel(file);
        setComponentAttributes();

        if (mode.equals("Normal"))
            BasicPacman.startPacman(pacmanAI, getStartNode(), gs, true);
        if (mode.equals("TestSearch"))
            testSearch();

        Main.game = this;
    }

    /**
     * Start graphical representation of used search-algorithm
     * @throws InterruptedException
     */
    private void testSearch() throws InterruptedException {
        JavaInterface ji = new JavaInterface(true, true);
        ji.startRenderer(800, 600);
        MyGoalTestFunction gtf = new MyGoalTestFunction();
        JavaGraphWrapper.drawGraph(getStartNode(), ji);
        Thread.sleep(10000);
        gs.search(getStartNode(), gtf);
    }

    private MyTileNode getStartNode() {
        for (MyTileNode n : level.getTileNodes().keySet()) {
            return n;
        }
        return null;
    }

    private void setComponentAttributes() {
        MyPacman pacman = level.getPacman();
        int goodiePower = 9000 / (level.getGoodieCounter() - 4);

        pacmanAI.setPacman(pacman);
        pacmanAI.setGoodiePower(goodiePower);
        pacmanAI.setLevel(level);
        int score = 0;
        pacmanAI.setScore(score);

        pacman.setLevel(level);

        for (MyGhost ghost : level.getGhosts()) {
            ghost.setPacman(pacman);
            ghost.setLevel(level);
        }
    }
}
