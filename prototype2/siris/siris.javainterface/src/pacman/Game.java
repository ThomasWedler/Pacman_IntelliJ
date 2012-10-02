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
    private MyPacman pacman;
    private int goodiePower;
    private int score = 0;

    private String mode = "Normal";

    private MyPacmanAI pacmanAI = new MyPacmanAI();
    private MyGraphSearch gs = new MyGraphSearch();

    public Game(File file) throws IOException, InterruptedException {
        level = new MyLevel(file);
        setComponentAttributes();

        if (mode.equals("Normal"))
            BasicPacman.startPacman(pacmanAI, getStartNode(), gs, true);
        if (mode.equals("TestSearch"))
            testSearch();
    }

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
        pacman = level.getPacman();
        goodiePower = 9000 / (level.getGoodieCounter() - 4);

        pacmanAI.setPacman(pacman);
        pacmanAI.setGoodiePower(goodiePower);
        pacmanAI.setLevel(level);
        pacmanAI.setScore(score);

        pacman.setLevel(level);

        for (MyGhost ghost : level.getGhosts()) {
            ghost.setPacman(pacman);
            ghost.setLevel(level);
        }
    }
}
