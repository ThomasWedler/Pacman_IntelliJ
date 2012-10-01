package pacman;
import java.io.IOException;
import siris.java.JavaInterface;
import siris.pacman.BasicPacman;
import siris.pacman.graph.JavaGraphWrapper;
import basic.MyGoalTestFunction;
import basic.MyGraphSearch;
import basic.MyLevel;
import basic.MyPacman;
import basic.MyPacmanAI;
import basic.MyTileNode;

public class Game {
	
	// Martin:
	// Editor: level direkt laden
		
	// Power-Ups wie im Original Pacman
	// ->> Geister weg von ihm
	
	public static MyLevel level;
	public static int score = 0;
	public static MyPacman pacman;
	public static int goodiePower = 0;
	public static int maximumPowerLevel = 9000;
	public static float gameSpeed = 0.5f;
	
	private String mode = "Normal";
	private int suddenDeathCounter = 5;
	
	public static void main(String[] args) throws IOException, InterruptedException {
		new Game("siris.javainterface/src/maps/Test.txt");
	}
	
	public Game(String levelname) throws IOException, InterruptedException {
		MyTileNode startNode = null;
		level = new MyLevel(levelname);
		pacman = level.getPacman();
		goodiePower = maximumPowerLevel / (level.getGoodieCounter() - suddenDeathCounter - 1);
		for (MyTileNode n : level.getTileNodes().keySet()) {
			startNode = n;
			break;
		}
		if (mode.equals("Normal"))
			start(startNode);
		if (mode.equals("TestSearch"))
			testSearch(startNode);
	}
	
	private void start(MyTileNode startNode) {
		MyGraphSearch gs = new MyGraphSearch();
		BasicPacman.startPacman(new MyPacmanAI(), startNode, gs, true);
	}
	
	private void testSearch(MyTileNode startNode) throws InterruptedException {
		JavaInterface ji = new JavaInterface(true, true);
		ji.startRenderer(800, 600);
		MyGraphSearch gs = new MyGraphSearch();
		MyGoalTestFunction gtf = new MyGoalTestFunction();
		JavaGraphWrapper.drawGraph(startNode, ji);
		Thread.sleep(10000);			
		gs.search(startNode, gtf);
	}

}
