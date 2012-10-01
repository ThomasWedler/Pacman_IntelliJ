package basic;

import java.util.LinkedList;
import pacman.Game;
import search.AStar;
import siris.pacman.graph.EntityNode;
import siris.pacman.graph.MovingEntityNode;

public class MyPacmanAI implements siris.pacman.PacmanAI {

	private String behaviour = "NORMAL";

	private int x = 0;
	private int y = 0;
	private int visionCounter = 0;

	@Override
	public void onSimulationStep(float deltaT) {
		//checkBulletTime();
	}

	@Override
	public void onDecisionRequired(MovingEntityNode entityToDecideFor) {
		boolean isSet = false;

		if (entityToDecideFor instanceof MyGhost) {
			MyGhost ghost = (MyGhost) entityToDecideFor;

				if (ghost.lookForPacman())
					ghost.setSeePacman(true);
				if (ghost.hearForPacman())
					ghost.setHearPacman(true);
				if (ghost.visionForPacman() && visionCounter > 10) {
					visionCounter = 0;
					ghost.setVisionPacman(true);
				}
				if (ghost.feelForPacman())
					ghost.setFeelPacman(true);
				
				if (checkSenses(ghost))
					ghost.setFound(true);
				
			if (behaviour.equals("DUMB")) {
				isSet = true;
				if (entityToDecideFor instanceof MyGhost)
					randomDirection((MyGhost) entityToDecideFor);
			} else if (behaviour.equals("ALLKNOWING")) {
				ghost.setDesiredPath(new AStar(ghost, Game.pacman).getResult());
			} else if (behaviour.equals("NORMAL")) {
				if (!ghost.getFound()) {
					if (ghost.getDesiredPath().isEmpty()) {
						randomDirection(ghost);
						isSet = true;
					}
				}
			}

			if (!isSet) {
				MyTileNode ghostPosition = ghost.getEntryNode();
				MyTileNode nextStep = null;
				String direction = "none";
				boolean set = false;
				if (ghostPosition.position().x() == ghost.getDesiredPath().getLast().position().x() && ghostPosition.position().y() == ghost.getDesiredPath().getLast().position().y()) {
					ghost.setDesiredPath(new LinkedList<MyTileNode>());
					ghost.setSpeed(3f * Game.gameSpeed);
				} else {
					for (int i = 0; i < ghost.getDesiredPath().size(); i++) {
						if (ghostPosition.position().x() == ghost.getDesiredPath().get(i).position().x() && ghostPosition.position().y() == ghost.getDesiredPath().get(i).position().y()) {
							nextStep = ghost.getDesiredPath().get(i + 1);
							direction = ghostPosition.getDifferenceBetweenPositions(nextStep);

							if (direction.equals("right"))
								x = 1;
							else if (direction.equals("left"))
								x = -1;
							else if (direction.equals("up"))
								y = 1;
							else if (direction.equals("down"))
								y = -1;

							ghost.setDesiredMovementDirection(x, y);
							set = true;
							x = 0;
							y = 0;
						}
					}
					if (!set) {
						MyTileNode pos = ghost.getDesiredPath().get(1);
						ghost.setDesiredMovementDirection(pos.position().x(), pos.position().y());
					}
				}
			}
			ghost.setFound(false);
		}
	}

	@Override
	public void onCollision(EntityNode e1, EntityNode e2) {
		if (e1 instanceof MyPacman) {
			if (e2 instanceof MyGhost) {
				System.out.println("Loser!");
				System.out.println("Your Score: " + Game.score);
				System.exit(0);
			} else if (e2 instanceof MyGoodie) {
				Game.level.setGoodieCounter(Game.level.getGoodieCounter() - 1);
				Game.score += Game.goodiePower;
				Game.pacman.setPowerLevel(Game.pacman.getPowerLevel() + Game.goodiePower);
				if (Game.level.getGoodieCounter() == 0) {
					System.out.println("Winner!");
					System.out.println("Your Score: " + Game.score);
					System.exit(0);
				}
			}
		}
	}

	private void randomDirection(MyGhost ghost) {
		boolean crossing = false;
		String s = Game.level.getTileNodes().get(ghost.getTileNode());
		if (s.equals("X") || s.equals("P") || s.equals("G"))
			crossing = true;

		if (crossing) {
			switch ((int) (Math.random() * (5 - 1) + 1)) {
				case 1:
					x = 1;
					break;
				case 2:
					y = 1;
					break;
				case 3:
					x = -1;
					break;
				case 4:
					y = -1;
					break;
			}

			MyTileNode ghostPosition = ghost.getEntryNode();
			if(ghostPosition != null) {
                boolean notFound = true;
                for (MyNode node : ghostPosition.getNeighbors()) {
                    if (node instanceof MyTileNode) {
                        MyTileNode n = (MyTileNode) node;
                        s = ghostPosition.getDifferenceBetweenPositions(n);
                        if ((s.equals("left") && x == -1) || (s.equals("right") && x == 1) || (s.equals("up") && y == 1) || (s.equals("down") && y == -1)) {
                            ghost.setDesiredMovementDirection(x, y);
                            notFound = false;
                        }
                    }
                }
                x = 0;
                y = 0;
                if (notFound)
                    randomDirection(ghost);
            }
		}
	}

	private boolean checkSenses(MyGhost ghost) {
		if (ghost.feelsPacman()) {
			ghost.setSpeed(3.6f * Game.gameSpeed);
			System.out.println("felt");
			ghost.setDesiredPath(new AStar(ghost, Game.pacman).getResult());
			ghost.setSeePacman(false);
			ghost.setHearPacman(false);
			ghost.setVisionPacman(false);
			ghost.setFeelPacman(false);
			return true;
		}
		if (ghost.visionsPacman()) {
			ghost.setSpeed(6f * Game.gameSpeed);
			System.out.println("visioned");
			ghost.setDesiredPath(new AStar(ghost, Game.pacman).getResult());
			ghost.setSeePacman(false);
			ghost.setHearPacman(false);
			ghost.setVisionPacman(false);
			ghost.setFeelPacman(false);
			return true;
		}
		if (ghost.seesPacman()) {
			ghost.setSpeed(3.6f * Game.gameSpeed);
			System.out.println("seen");
			ghost.setDesiredPath(new AStar(ghost, Game.pacman).getResult());
			ghost.setSeePacman(false);
			ghost.setHearPacman(false);
			ghost.setVisionPacman(false);
			ghost.setFeelPacman(false);
			return true;
		}
		if (ghost.hearsPacman()) {
			ghost.setSpeed(2.4f * Game.gameSpeed);
			System.out.println("heard");
			ghost.setDesiredPath(new AStar(ghost, Game.pacman).getResult());
			ghost.setSeePacman(false);
			ghost.setHearPacman(false);
			ghost.setVisionPacman(false);
			ghost.setFeelPacman(false);
			return true;
		}
		return false;
	}

	private void checkBulletTime() {
		boolean close = false;
		MyTileNode pacmanNode = (MyTileNode) Game.pacman.getTileNode();

		for (MyNode n : pacmanNode.neighbors()) {
			if (n instanceof MyTileNode) {
				MyTileNode ghostNode = (MyTileNode) n;
				for (MyGhost ghost : Game.level.getGhosts()) {
					if (ghost.getTileNode().equals(ghostNode)) {
						close = true;
					}
				}
			}
		}

		if (close) {
			Game.pacman.setSpeed(1.5f);
			Game.gameSpeed = Game.gameSpeed/2;
		} else {
			Game.pacman.setSpeed(3f);
			Game.gameSpeed = Game.gameSpeed/2;
		}
	}

}
