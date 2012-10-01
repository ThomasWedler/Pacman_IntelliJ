package basic;

import java.util.LinkedList;
import pacman.Game;

public class MyGhost extends MyMovingEntityNode implements siris.pacman.graph.Ghost {

	private LinkedList<MyTileNode> desiredPath = new LinkedList<MyTileNode>();
	private int number = 0;
	private boolean seePacman = false;
	private boolean hearPacman = false;
	private boolean visionPacman = false;
	private boolean feelPacman = false;
	private int hearRange = 1;
	private int visionRange = 2;
	private boolean found = false;

	public void setNr(int x) {
		number = x;
	}

	@Override
	public int getNr() {
		return number;
	}

	public boolean lookForPacman() {
		MyTileNode position = (MyTileNode) this.getEntryNode();
		if (position != null) {
			for (MyNode n : position.getNeighbors()) {
				if (n instanceof MyPacman)
					return true;
				if (n instanceof MyTileNode) {
					MyTileNode node = (MyTileNode) n;
					String direction = position.getDifferenceBetweenPositions(node);
					if (direction.equals("left") && !this.getDirection().equals("right")) {
						for (MyTileNode tilenode : Game.level.getTileNodes().keySet()) {
							for (int i = 0; i < 100; i++) {
								if (tilenode.position().x() == node.position().x() - i && tilenode.position().y() == node.position().y()) {
									if (Game.pacman.getTileNode().position().x() == tilenode.position().x() && Game.pacman.getTileNode().position().y() == tilenode.position().y())
										return true;
								}
							}
						}
					}
					if (direction.equals("right") && !this.getDirection().equals("left")) {
						for (MyTileNode tilenode : Game.level.getTileNodes().keySet()) {
							for (int i = 0; i < 100; i++) {
								if (tilenode.position().x() == node.position().x() + i && tilenode.position().y() == node.position().y()) {
									if (Game.pacman.getTileNode().position().x() == tilenode.position().x() && Game.pacman.getTileNode().position().y() == tilenode.position().y())
										return true;
								}
							}
						}
					}
					if (direction.equals("up") && !this.getDirection().equals("down")) {
						for (MyTileNode tilenode : Game.level.getTileNodes().keySet()) {
							for (int i = 0; i < 100; i++) {
								if (tilenode.position().x() == node.position().x() && tilenode.position().y() == node.position().y() + i) {
									if (Game.pacman.getTileNode().position().x() == tilenode.position().x() && Game.pacman.getTileNode().position().y() == tilenode.position().y())
										return true;
								}
							}
						}
					}
					if (direction.equals("down") && !this.getDirection().equals("up")) {
						for (MyTileNode tilenode : Game.level.getTileNodes().keySet()) {
							for (int i = 0; i < 100; i++) {
								if (tilenode.position().x() == node.position().x() && tilenode.position().y() == node.position().y() - i) {
									if (Game.pacman.getTileNode().position().x() == tilenode.position().x() && Game.pacman.getTileNode().position().y() == tilenode.position().y())
										return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	public boolean hearForPacman() {
		int xMax = this.getTileNode().position().x() + hearRange;
		int xMin = this.getTileNode().position().x() - hearRange;
		int yMax = this.getTileNode().position().y() + hearRange;
		int yMin = this.getTileNode().position().y() - hearRange;
		for (MyTileNode node : Game.level.getTileNodes().keySet()) {
			if (node.position().x() <= xMax && node.position().x() >= xMin && node.position().y() <= yMax && node.position().y() >= yMin) {
				if (Game.pacman.getTileNode().position().equals(node.position()))
					return true;
			}
		}
		return false;
	}

	public boolean visionForPacman() {
		int counter = 0;
		int random = (int) (Math.random() * Game.level.getTileNodes().keySet().size());
		MyTileNode randomNode = null;
		for (MyTileNode node : Game.level.getTileNodes().keySet()) {
			if (counter == random) {
				randomNode = node;
				break;
			} else
				counter++;
		}
		int xMax = randomNode.position().x() + visionRange;
		int xMin = randomNode.position().x() - visionRange;
		int yMax = randomNode.position().y() + visionRange;
		int yMin = randomNode.position().y() - visionRange;
		for (MyTileNode node : Game.level.getTileNodes().keySet()) {
			if (node.position().x() <= xMax && node.position().x() >= xMin && node.position().y() <= yMax && node.position().y() >= yMin) {
				if (Game.pacman.getTileNode().position().equals(node.position()))
					return true;
			}
		}
		return false;
	}

	private boolean power = true;

	public boolean feelForPacman() {
		if (Game.pacman.getPowerLevel() > Game.maximumPowerLevel) {
			if (power) {
				System.out.println("Over 9000!!!");
				power = false;
			}
			return true;
		}
		return false;
	}

	public MyTileNode getEntryNode() {
		int x = this.getTileNode().position().x() + this.getCurrentMovementDirectionX();
		int y = this.getTileNode().position().y() + this.getCurrentMovementDirectionY();
		for (MyTileNode node : Game.level.getTileNodes().keySet()) {
			if (node.position().x() == x && node.position().y() == y)
				return node;
		}
		return null;
	}

	public LinkedList<MyTileNode> getDesiredPath() {
		return desiredPath;
	}

	public void setDesiredPath(LinkedList<MyTileNode> desiredPath) {
		this.desiredPath = desiredPath;
	}

	public boolean seesPacman() {
		return seePacman;
	}

	public boolean hearsPacman() {
		return hearPacman;
	}

	public boolean visionsPacman() {
		return visionPacman;
	}

	public boolean feelsPacman() {
		return feelPacman;
	}

	public void setSeePacman(boolean seePacman) {
		this.seePacman = seePacman;
	}

	public void setHearPacman(boolean hearPacman) {
		this.hearPacman = hearPacman;
	}

	public void setVisionPacman(boolean visionPacman) {
		this.visionPacman = visionPacman;
	}

	public void setFeelPacman(boolean feelPacman) {
		this.feelPacman = feelPacman;
	}

	public boolean getFound() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}

}
