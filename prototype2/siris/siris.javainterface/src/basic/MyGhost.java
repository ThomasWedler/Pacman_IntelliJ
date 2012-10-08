package basic;

import siris.pacman.BasicPacman;

import java.util.LinkedList;

public class MyGhost extends MyMovingEntityNode implements siris.pacman.graph.Ghost {

    private LinkedList<MyTileNode> desiredPath = new LinkedList<MyTileNode>();

    // Set all senses to false
    private boolean seePacman = false;
    private boolean hearPacman = false;
    private boolean visionPacman = false;
    private boolean feelPacman = false;

    private float hearRange = 1.5f;
    private float visionRange = 2f;

    // Vision timer is for checking if visions may occur.
    private float visionTimer = 0f;

    private boolean hasRandomed = false;
    private boolean foundPacman = false;

    private boolean freeze = false;
    private float freezeTimer = 0f;

    /**
     * Check all senses of ghost
     *
     * @param bulletTime to check if bulletTime is set
     * @return if Pacman is sensed by one of the ghost's senses
     */
    public boolean checkSenses(boolean bulletTime) {
        if (feelsPacman()) {
            foundPacman = true;
            if (!bulletTime && !getPacman().isPoweredUp())
                setSpeed(1.2f);
            System.out.println("felt");
            setSensesFalse();
            return true;
        }
        if (visionsPacman()) {
            foundPacman = true;
            if (!bulletTime && !getPacman().isPoweredUp())
                setSpeed(2f);
            System.out.println("visioned");
            setSensesFalse();
            return true;
        }
        if (seesPacman()) {
            foundPacman = true;
            if (!bulletTime && !getPacman().isPoweredUp())
                setSpeed(1.2f);
            System.out.println("seen");
            setSensesFalse();
            return true;
        }
        if (hearsPacman()) {
            foundPacman = true;
            if (!bulletTime && !getPacman().isPoweredUp())
                setSpeed(0.6f);
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

    private LinkedList<MyTileNode> pathToCheck;

    /**
     * Check if Pacman can be seen by ghost on its position.
     *
     * @return if Pacman is seen
     */
    public boolean lookForPacman() {
        MyTileNode pacmanNode = getPacman().getTileNode();
        pathToCheck = new LinkedList<MyTileNode>();
        MyTileNode startingPosition = getTileNode();

        if (getDirection().equals("left")) {
            look(startingPosition, "left");
            look(startingPosition, "up");
            look(startingPosition, "down");
        } else if (getDirection().equals("right")) {
            look(startingPosition, "right");
            look(startingPosition, "up");
            look(startingPosition, "down");
        } else if (getDirection().equals("up")) {
            look(startingPosition, "left");
            look(startingPosition, "up");
            look(startingPosition, "right");
        } else if (getDirection().equals("down")) {
            look(startingPosition, "left");
            look(startingPosition, "right");
            look(startingPosition, "down");
        }

        for (MyTileNode nodeToCheck : pathToCheck) {
            if (pacmanNode.getDifferenceBetweenPositions(nodeToCheck).equals("none"))
                return true;
        }

        return false;
    }

    /**
     * Look in direction until sight is blocked.
     *
     * @param position  of where to look from
     * @param direction in where to look to
     */
    private void look(MyTileNode position, String direction) {
        for (MyNode n : position.getNeighbors()) {
            if (n instanceof MyTileNode) {
                MyTileNode neighbourNode = (MyTileNode) n;
                String dir = position.getDifferenceBetweenPositions(neighbourNode);
                if (direction.equals(dir)) {
                    pathToCheck.add(neighbourNode);
                    look(neighbourNode, direction);
                }

            }
        }
    }

    /**
     * Check if Pacman can be heard by ghost.
     *
     * @return if Pacman can be heard.
     */
    public boolean hearForPacman() {
        float c = rangeCheck(getPositionX(), getPositionY());
        return c < hearRange;
    }

    /**
     * Check if Pacman can be visioned by ghost.
     *
     * @param elapsed time
     * @return if Pacman is visioned
     */
    public boolean visionForPacman(float elapsed) throws InterruptedException {
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
            if (c < visionRange) {
                if (!getPacman().isPoweredUp() && !feelsPacman()) {
                    BasicPacman.setColorToRed();
                    Thread.sleep(100);
                    BasicPacman.setColorToNormal();
                }
                return true;
            }
        }

        return false;
    }

    private float rangeCheck(float x, float y) {
        float a = getPacman().getPositionX() - x;
        float b = getPacman().getPositionY() - y;
        return (float) Math.sqrt((a * a) + (b * b));
    }

    /**
     * If power level of Pacman is > 9000 ghosts can feel Pacman.
     *
     * @return if power level is over 9000
     */
    public boolean feelForPacman() {
        return getPacman().getPowerLevel() > 9000;
    }

    /**
     * Randomize ghost's movement.
     *
     * @param direction where ghost comes from to avoid going back
     */
    public void randomDirection(String direction) {
        int x = 0;
        int y = 0;
        String s = getLevel().getTileNodes().get(getTileNode());
        boolean notFound = true;

        // Change direction only on crossings (because P and G positions CAN be
        // crossings but is not checked here, changing of direction is allowed on this
        // positions as well)
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

            for (MyNode node : getTileNode().getNeighbors()) {
                if (node instanceof MyTileNode) {
                    MyTileNode neighbourNode = (MyTileNode) node;
                    s = getTileNode().getDifferenceBetweenPositions(neighbourNode);

                    if (direction.equals("none")) {
                        if ((s.equals("left") && x == -1) || (s.equals("up") && y == 1) || (s.equals("down") && y == -1) || (s.equals("right") && x == 1)) {
                            setDesiredMovementDirection(x, y);
                            notFound = false;
                        }
                    }
                    if (direction.equals("left")) {
                        if ((s.equals("left") && x == -1) || (s.equals("up") && y == 1) || (s.equals("down") && y == -1)) {
                            setDesiredMovementDirection(x, y);
                            notFound = false;
                        }
                    }
                    if (direction.equals("right")) {
                        if ((s.equals("right") && x == 1) || (s.equals("up") && y == 1) || (s.equals("down") && y == -1)) {
                            setDesiredMovementDirection(x, y);
                            notFound = false;
                        }
                    }
                    if (direction.equals("up")) {
                        if ((s.equals("left") && x == -1) || (s.equals("up") && y == 1) || (s.equals("right") && x == 1)) {
                            setDesiredMovementDirection(x, y);
                            notFound = false;
                        }
                    }
                    if (direction.equals("down")) {
                        if ((s.equals("left") && x == -1) || (s.equals("right") && x == 1) || (s.equals("down") && y == -1)) {
                            setDesiredMovementDirection(x, y);
                            notFound = false;
                        }
                    }
                }
            }
            if (notFound)
                randomDirection(getDirection());
        }

        hasRandomed = true;
    }

    public void flee(String direction) {
        int x = 0;
        int y = 0;
        boolean notFound = true;

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

        for (MyNode node : getTileNode().getNeighbors()) {
            if (node instanceof MyTileNode) {
                MyTileNode neighbourNode = (MyTileNode) node;
                String s = getTileNode().getDifferenceBetweenPositions(neighbourNode);

                if (direction.equals("left")) {
                    if ((s.equals("left") && x == -1) || (s.equals("up") && y == 1) || (s.equals("down") && y == -1)) {
                        setDesiredMovementDirection(x, y);
                        notFound = false;
                    }
                }
                if (direction.equals("right")) {
                    if ((s.equals("right") && x == 1) || (s.equals("up") && y == 1) || (s.equals("down") && y == -1)) {
                        setDesiredMovementDirection(x, y);
                        notFound = false;
                    }
                }
                if (direction.equals("up")) {
                    if ((s.equals("left") && x == -1) || (s.equals("up") && y == 1) || (s.equals("right") && x == 1)) {
                        setDesiredMovementDirection(x, y);
                        notFound = false;
                    }
                }
                if (direction.equals("down")) {
                    if ((s.equals("left") && x == -1) || (s.equals("right") && x == 1) || (s.equals("down") && y == -1)) {
                        setDesiredMovementDirection(x, y);
                        notFound = false;
                    }
                }
            }
            if (notFound)
                randomDirection(getDirection());
        }

        hasRandomed = true;
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

    public float getFreezeTimer() {
        return freezeTimer;
    }

    public void setFreezeTimer(float freezeTimer) {
        this.freezeTimer = freezeTimer;
    }

    public boolean getFreeze() {
        return freeze;
    }

    public void setFreeze(boolean freeze) {
        this.freeze = freeze;
    }

}
