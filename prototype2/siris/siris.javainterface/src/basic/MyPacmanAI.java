package basic;

import search.AStar;
import siris.pacman.graph.EntityNode;
import siris.pacman.graph.MovingEntityNode;

import java.util.LinkedList;

public class MyPacmanAI implements siris.pacman.PacmanAI {

    private MyPacman pacman;
    private int score;
    private MyLevel level;
    private int goodiePower;

    private String behaviour = "NORMAL";

    @Override
    public void onSimulationStep(float deltaT) {
        pacman.checkForBulletTime();

        for (MyGhost ghost : level.getGhosts()) {
            if (ghost.lookForPacman())
                ghost.setSeePacman(true);
           /* if (ghost.hearForPacman())
                ghost.setHearPacman(true);
            if (ghost.visionForPacman(deltaT))
                ghost.setVisionPacman(true);
            if (ghost.feelForPacman())
                ghost.setFeelPacman(true); */

            ghost.checkSenses();
        }
    }

    @Override
    public void onDecisionRequired(MovingEntityNode entityToDecideFor) {
        if (entityToDecideFor instanceof MyGhost) {
            MyGhost ghost = (MyGhost) entityToDecideFor;

            MyTileNode ghostPosition = ghost.getTileNode();
            int ghostPositionX = ghostPosition.position().x();
            int ghostPositionY = ghostPosition.position().y();

            if (ghost.getFoundPacman())
                ghost.setDesiredPath(new AStar(ghost, pacman).getResult());

            LinkedList<MyTileNode> desiredPath = ghost.getDesiredPath();

            if (behaviour.equals("DUMB")) {
                if (entityToDecideFor instanceof MyGhost)
                    ghost.randomDirection();
            } else if (behaviour.equals("ALLKNOWING")) {
                ghost.setDesiredPath(new AStar(ghost, pacman).getResult());
            } else if (behaviour.equals("NORMAL")) {
                if (!ghost.getFoundPacman()) {
                    if (desiredPath.isEmpty()) {
                        ghost.randomDirection();
                        ghost.setSpeed(1f);
                    }
                }
            }

            desiredPath = ghost.getDesiredPath();

            if (!ghost.decidedRandom()) {
                int newX = 0;
                int newY = 0;

                if (ghost.onLastPathStep()) {
                    ghost.setDesiredPath(new LinkedList<MyTileNode>());
                    ghost.setSpeed(1f);
                } else {
                    for (int i = 0; i < ghost.getDesiredPath().size(); i++) {
                        if (ghostPositionX == desiredPath.get(i).position().x() && ghostPositionY == desiredPath.get(i).position().y()) {
                            MyTileNode nextStep = desiredPath.get(i + 1);
                            String direction = ghostPosition.getDifferenceBetweenPositions(nextStep);

                            if (direction.equals("right"))
                                newX = 1;
                            else if (direction.equals("left"))
                                newX = -1;
                            else if (direction.equals("up"))
                                newY = 1;
                            else if (direction.equals("down"))
                                newY = -1;

                            ghost.setDesiredMovementDirection(newX, newY);
                        }
                    }
                }
            }

            ghost.setFoundPacman(false);
            ghost.setDecidedRandom(false);
        }
    }

    @Override
    public void onCollision(EntityNode e1, EntityNode e2) {
        if (e1 instanceof MyPacman) {
            if (e2 instanceof MyGhost) {
                System.out.println("Loser!");
                System.out.println("Your Score: " + score);
                System.exit(0);
            } else if (e2 instanceof MyGoodie) {
                level.setGoodieCounter(level.getGoodieCounter() - 1);
                score += goodiePower;
                pacman.setPowerLevel(pacman.getPowerLevel() + goodiePower);
                if (level.getGoodieCounter() == 0) {
                    System.out.println("Winner!");
                    System.out.println("Your Score: " + score);
                    System.exit(0);
                }
            }
        }
    }

    public void setPacman(MyPacman pacman) {
        this.pacman = pacman;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setLevel(MyLevel level) {
        this.level = level;
    }

    public void setGoodiePower(int goodiePower) {
        this.goodiePower = goodiePower;
    }

}
