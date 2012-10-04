package basic;

import java.util.LinkedList;

public class MyGhost extends MyMovingEntityNode implements siris.pacman.graph.Ghost {

    private LinkedList<MyTileNode> desiredPath = new LinkedList<MyTileNode>();

    private boolean seePacman = false;
    private boolean hearPacman = false;
    private boolean visionPacman = false;
    private boolean feelPacman = false;

    private float hearRange = 1f;
    private float visionRange = 2f;
    private float visionTimer = 0f;

    private boolean hasRandomed = false;
    private boolean foundPacman = false;

    public boolean checkSenses(boolean bulletTime) {
        if (feelsPacman()) {
            foundPacman = true;
            if (!bulletTime)
                setSpeed(1.2f);
            System.out.println("felt");
            setSensesFalse();
            return true;
        }
        if (visionsPacman()) {
            foundPacman = true;
            if (!bulletTime)
                setSpeed(2f);
            System.out.println("visioned");
            setSensesFalse();
            return true;
        }
        if (seesPacman()) {
            foundPacman = true;
            if (!bulletTime)
                setSpeed(1.2f);
            System.out.println("seen");
            setSensesFalse();
            return true;
        }
        if (hearsPacman()) {
            foundPacman = true;
            if (!bulletTime)
                setSpeed(0.8f);
            System.out.println("heard");
            setSensesFalse();
            return true;
        }
        return false;
    }

    private void setSensesFalse() {
        setSeePacman(false);
        setHearPacman(false);
        setVisionPacman(false);
        setFeelPacman(false);
    }

    public boolean lookForPacman() {
        MyTileNode position = getTileNode();
        for (MyNode n : position.getNeighbors()) {
            if (n instanceof MyPacman)
                return true;

            if (n instanceof MyTileNode) {
                MyTileNode neighbourNode = (MyTileNode) n;
                String direction = position.getDifferenceBetweenPositions(neighbourNode);

                int neighbourNodeX = neighbourNode.position().x();
                int neighbourNodeY = neighbourNode.position().y();
                MyTileNode pacmanNode = getPacman().getTileNode();

                if (direction.equals("left") && !getDirection().equals("right")) {
                    for (MyTileNode nodeToCheck : getLevel().getTileNodes().keySet()) {
                        for (int i = 0; i < 100; i++) {
                            if (nodeToCheck.position().x() == neighbourNodeX - i && nodeToCheck.position().y() == neighbourNodeY) {
                                if (pacmanNode.getDifferenceBetweenPositions(nodeToCheck).equals("none"))
                                    return true;
                            }
                        }
                    }
                }
                if (direction.equals("right") && !getDirection().equals("left")) {
                    for (MyTileNode nodeToCheck : getLevel().getTileNodes().keySet()) {
                        for (int i = 0; i < 100; i++) {
                            if (nodeToCheck.position().x() == neighbourNodeX + i && nodeToCheck.position().y() == neighbourNodeY) {
                                if (pacmanNode.getDifferenceBetweenPositions(nodeToCheck).equals("none"))
                                    return true;
                            }
                        }
                    }
                }
                if (direction.equals("up") && !getDirection().equals("down")) {
                    for (MyTileNode nodeToCheck : getLevel().getTileNodes().keySet()) {
                        for (int i = 0; i < 100; i++) {
                            if (nodeToCheck.position().x() == neighbourNodeX && nodeToCheck.position().y() == neighbourNodeY + i) {
                                if (pacmanNode.getDifferenceBetweenPositions(nodeToCheck).equals("none"))
                                    return true;
                            }
                        }
                    }
                }
                if (direction.equals("down") && !getDirection().equals("up")) {
                    for (MyTileNode nodeToCheck : getLevel().getTileNodes().keySet()) {
                        for (int i = 0; i < 100; i++) {
                            if (nodeToCheck.position().x() == neighbourNodeX && nodeToCheck.position().y() == neighbourNodeY - i) {
                                if (pacmanNode.getDifferenceBetweenPositions(nodeToCheck).equals("none"))
                                    return true;
                            }
                        }
                    }
                }
            }

        }
        return false;
    }


 /*   public boolean lookForPacman() {
        boolean result = false;
        boolean doSearch = false;
        MyTileNode startingPosition = getTileNode();
        for (MyNode n : startingPosition.getNeighbors()) {
            if (n instanceof MyTileNode) {
                MyTileNode neighbourNode = (MyTileNode) n;
                String direction = startingPosition.getDifferenceBetweenPositions(neighbourNode);
                if (getDirection().equals("left") && !direction.equals("right")) {
                    doSearch = true;
                } else if (getDirection().equals("right") && !direction.equals("left")) {
                    doSearch = true;
                } else if (getDirection().equals("up") && !direction.equals("down")) {
                    doSearch = true;
                } else if (getDirection().equals("down") && !direction.equals("up")) {
                    doSearch = true;
                }
                if (doSearch) {
                    if (lookOut(neighbourNode, direction))
                        result = true;
                }
            }
        }
        return result;
    }

    private boolean lookOut(MyTileNode position, String direction) {
        for (MyNode n : position.getNeighbors()) {
            if (n instanceof MyPacman)
                return true;

            if (n instanceof MyTileNode) {
                MyTileNode neighbourNode = (MyTileNode) n;
                String dir = position.getDifferenceBetweenPositions(neighbourNode);

                if (direction.equals("left") && dir.equals("left"))
                    return lookOut(neighbourNode, dir);
                if (direction.equals("right") && dir.equals("right"))
                    return lookOut(neighbourNode, dir);
                if (direction.equals("up") && dir.equals("up"))
                    return lookOut(neighbourNode, dir);
                if (direction.equals("down") && dir.equals("down"))
                    return lookOut(neighbourNode, dir);
            }
        }

        return false;
    }  */

    public boolean hearForPacman() {
        float c = rangeCheck(getPositionX(), getPositionY());
        return c < hearRange;
    }

    public boolean visionForPacman(float elapsed) {
        visionTimer += elapsed;

        if (visionTimer > 10f) {
            visionTimer = 0f;
            int counter = 0;
            int random = (int) (Math.random() * getLevel().getTileNodes().keySet().size());
            float x = 0;
            float y = 0;

            for (MyTileNode node : getLevel().getTileNodes().keySet()) {
                if (counter == random) {
                    x = node.position().x();
                    y = node.position().y();
                    break;
                } else
                    counter++;
            }

            float c = rangeCheck(x, y);
            if (c < visionRange)
                return true;
        }

        return false;
    }

    private float rangeCheck(float x, float y) {
        float a = getPacman().getPositionX() - x;
        float b = getPacman().getPositionY() - y;
        return (float) Math.sqrt((a * a) + (b * b));
    }

    public boolean feelForPacman() {
        return getPacman().getPowerLevel() > 9000;
    }

    public void randomDirection() {
        int x = 0;
        int y = 0;
        String s = getLevel().getTileNodes().get(getTileNode());
        boolean notFound = true;

        if (s.equals("X") || s.equals("P") || s.equals("G")) {
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

            if (getTileNode() != null) {
                for (MyNode node : getTileNode().getNeighbors()) {
                    if (node instanceof MyTileNode) {
                        MyTileNode n = (MyTileNode) node;
                        s = getTileNode().getDifferenceBetweenPositions(n);
                        if ((s.equals("left") && x == -1) || (s.equals("right") && x == 1) || (s.equals("up") && y == 1) || (s.equals("down") && y == -1)) {
                            setDesiredMovementDirection(x, y);
                            notFound = false;
                        }
                    }
                }
                if (notFound)
                    randomDirection();
            }
        }
        setDecidedRandom(true);
    }

    public boolean onLastPathStep() {
        MyTileNode position = getTileNode();
        MyTileNode lastTileNode = desiredPath.getLast();
        String result = position.getDifferenceBetweenPositions(lastTileNode);
        return result.equals("none");
    }

    public LinkedList<MyTileNode> getDesiredPath() {
        return desiredPath;
    }

    public void setDesiredPath(LinkedList<MyTileNode> desiredPath) {
        this.desiredPath = desiredPath;
    }

    boolean seesPacman() {
        return seePacman;
    }

    boolean hearsPacman() {
        return hearPacman;
    }

    boolean visionsPacman() {
        return visionPacman;
    }

    boolean feelsPacman() {
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

    public boolean getFoundPacman() {
        return foundPacman;
    }

    public void setFoundPacman(boolean foundPacman) {
        this.foundPacman = foundPacman;
    }

    @Override
    public int getNr() {
        return 0;
    }

    public boolean decidedRandom() {
        return hasRandomed;
    }

    public void setDecidedRandom(boolean hasRandomed) {
        this.hasRandomed = hasRandomed;
    }

}
