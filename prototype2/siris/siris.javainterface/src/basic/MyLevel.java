package basic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;

import siris.pacman.util.TilePosition;

public class MyLevel {

	private HashMap<MyTileNode, String> tileNodes = new HashMap<MyTileNode, String>();
	private LinkedList<MyGhost> ghosts = new LinkedList<MyGhost>();
	private int goodieCounter = 0;
	private MyPacman pacman;
	
	public MyLevel(File file) throws IOException {
		renderLevel(file);
	}

    /**
     * Level parser
     * @param file of level to parse
     * @throws IOException if file doesn't exist or isn't readable
     */
	public void renderLevel(File file) throws IOException {
		int col = 0;
		int row = countRows(file);
		
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String str;
		
		while ((str = br.readLine()) != null) {
			String[] parts = str.split("(?<=\\G.)");
			for (int i = 0; i < parts.length; i++) {
				if (!parts[i].equals(" ")) {
					createTile(parts[i], row, col);
				}
				col++;
			}
			row--;
			col = 0;
		}
		
		br.close();
	}

    /**
     * Create tile node for each valid item in parsed file
     * @param s Item
     * @param row of item
     * @param col of item
     */
	public void createTile(String s, int row, int col) {
		MyTileNode n = new MyTileNode();
		n.setPosition(new TilePosition(col, row));
		tileNodes.put(n, s);

		TilePosition posleft = new TilePosition(col - 1, row);
		TilePosition posup = new TilePosition(col, row + 1);
		for (MyTileNode node : tileNodes.keySet()) {
			if (node.position().x() == posleft.x()
					&& node.position().y() == posleft.y()) {
				n.connectTo(node);
			}
			if (node.position().x() == posup.x()
					&& node.position().y() == posup.y()) {
				n.connectTo(node);
			}
		}

		if (s.equals("P")) {
			MyPacman pacman = new MyPacman();
			pacman.setTileNode(n);
			pacman.setPosition(col, row);
			if (this.pacman == null)
				this.pacman = pacman;
			else
				System.out.println("There can be only one Pacman!");
		}
		
		if (s.equals("G")) {
			MyGhost ghost = new MyGhost();
			ghost.setTileNode(n);
			ghost.setPosition(col, row);
			ghosts.add(ghost);
		}

		if (s.equals("-") || s.equals("X") || s.equals("I")) {
			MyGoodie goodie = new MyGoodie();
			goodie.setTileNode(n);
			goodie.setPosition(col, row);
			goodieCounter++;
		}
	}

    /**
     * Level parser to count rows in level-file
     * @param file of level
     * @return number of rows
     * @throws IOException
     */
	public int countRows(File file) throws IOException {
        // Set row count to -1 to set row to 0 by first row++ call
        int row = -1;
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		while ((br.readLine()) != null) {
			row++;
		}
		br.close();
		return row;
	}

    public HashMap<MyTileNode, String> getTileNodes() {
        return tileNodes;
    }

    public LinkedList<MyGhost> getGhosts() {
        return ghosts;
    }

    public MyPacman getPacman() {
        return pacman;
    }

    public int getGoodieCounter() {
        return goodieCounter;
    }

    public void setGoodieCounter(int goodieCounter) {
        this.goodieCounter = goodieCounter;
    }

}
