package basic;

import search.AStar;
import siris.pacman.BasicPacman;
import siris.pacman.graph.EntityNode;
import siris.pacman.graph.MovingEntityNode;

import javax.swing.*;
import java.util.LinkedList;

public class MyPacmanAI implements siris.pacman.PacmanAI {

    private MyPacman pacman;
    private int score;
    private MyLevel level;
    private int goodiePower;

    private boolean freeze = false;
    private float freezeTimer = 0f;

    private String behaviour = "NORMAL";

    @Override
    public void onSimulationStep(float deltaT) {
        boolean bulletTime = false;

        pacman.checkForPowerUp(deltaT);

        for (MyGhost ghost : level.getGhosts()) {
            try {
                if (ghost.lookForPacman())
                    ghost.setSeePacman(true);
                if (ghost.hearForPacman())
                    ghost.setHearPacman(true);
                if (ghost.visionForPacman(deltaT))
                    ghost.setVisionPacman(true);
                if (ghost.feelForPacman() && !pacman.isPoweredUp()) {
                    ghost.setFeelPacman(true);
                    BasicPacman.setColorToRed();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (!pacman.isPoweredUp())
                bulletTime = pacman.checkForBulletTime();
            else {
                pacman.setSpeed(1f);
                ghost.setSpeed(0.5f);
            }

            ghost.checkSenses(bulletTime);

            if (freeze) {
                freezeTimer += deltaT;
                ghost.setSpeed(0f);
            }
            if (freezeTimer > 5f) {
                freezeTimer = 0f;
                freeze = false;
                ghost.randomDirection(ghost.getDirection());
                ghost.setSpeed(1f);
            }
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

            if (pacman.isPoweredUp()) {
                ghost.setDesiredPath(new AStar(ghost, pacman).getResult());
                MyTileNode firstStep = ghost.getDesiredPath().get(1);
                String direction = ghostPosition.getDifferenceBetweenPositions(firstStep);
                System.out.println(direction);
                if (direction.equals("left"))
                    ghost.flee("right");
                else if (direction.equals("right"))
                    ghost.flee("left");
                else if (direction.equals("up"))
                    ghost.flee("down");
                else if (direction.equals("down"))
                    ghost.flee("up");
                ghost.setFoundPacman(false);
            }

            LinkedList<MyTileNode> desiredPath = ghost.getDesiredPath();

            if (behaviour.equals("DUMB")) {
                ghost.randomDirection(ghost.getDirection());
            } else if (behaviour.equals("ALLKNOWING")) {
                ghost.setDesiredPath(new AStar(ghost, pacman).getResult());
            } else if (behaviour.equals("NORMAL")) {
                if (!ghost.getFoundPacman()) {
                    if (desiredPath.isEmpty()) {
                        ghost.randomDirection(ghost.getDirection());
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
                if (pacman.isPoweredUp()) {
                    if (!freeze)
                        score += 500;
                    freeze = true;
                } else {
                    JOptionPane.showMessageDialog(null, "You lost!\nYour Score: " + score, "Loser!", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
            } else if (e2 instanceof MyGoodie) {
                level.setGoodieCounter(level.getGoodieCounter() - 1);
                score += ((MyGoodie) e2).getGoodieScore();
                pacman.setPowerLevel(pacman.getPowerLevel() + goodiePower);
                if (level.getGoodieCounter() == 0) {
                    JOptionPane.showMessageDialog(null, "You won!\nYour Score: " + score, "Winner!", JOptionPane.INFORMATION_MESSAGE);
                    System.exit(0);
                }
            } else if (e2 instanceof MyPowerUp) {
                BasicPacman.setColorToBlue();
                for (MyGhost ghost : level.getGhosts()) {
                    ghost.setDesiredPath(new LinkedList<MyTileNode>());
                }
                pacman.setPowerUpTimer(0f);
                pacman.setPoweredUp(true);
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
