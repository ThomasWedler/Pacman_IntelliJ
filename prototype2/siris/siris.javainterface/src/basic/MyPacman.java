package basic;

import java.util.LinkedList;

public class MyPacman extends MyMovingEntityNode implements siris.pacman.graph.Pacman {

    private int powerLevel = 1;

    public boolean checkForBulletTime() {
        float slowFactor = 1f;

        LinkedList<Float> distances = new LinkedList<Float>();

        for (MyGhost ghost : getLevel().getGhosts()) {
            float a = ghost.getPositionX() - getPositionX();
            float b = ghost.getPositionY() - getPositionY();
            float c = (float) Math.sqrt((a * a) + (b * b));
            distances.add(c);
        }

        float minValue = 0;
        for (float f : distances) {
            if (minValue == 0)
                minValue = f;
            else if (f < minValue) {
                minValue = f;
            }
        }

        if (minValue > 0f && minValue <= 0.5f)
            slowFactor = 0.1f;
        else if (minValue > 0.5f && minValue < 1f)
            slowFactor = 0.25f;
        else if (minValue > 1f && minValue < 1.5f)
            slowFactor = 0.5f;
        else if (minValue > 1.5f && minValue <= 2f)
            slowFactor = 0.75f;
        else if (minValue > 2f)
            slowFactor = 1f;

        if (slowFactor == 0.1f && getSpeed() != 0.1f)
            setSpeed(0.1f);
        else if (slowFactor == 0.25f && getSpeed() != 0.25f)
            setSpeed(0.25f);
        else if (slowFactor == 0.5f && getSpeed() != 0.5f)
            setSpeed(0.5f);
        else if (slowFactor == 0.75f && getSpeed() != 0.75f)
            setSpeed(0.75f);
        else if (slowFactor == 1f && getSpeed() != 1f)
            setSpeed(1f);

        for (MyGhost ghost : getLevel().getGhosts()) {
            if (slowFactor == 0.1f && ghost.getSpeed() != 0.1f)
                ghost.setSpeed(0.1f);
            else if (slowFactor == 0.25f && ghost.getSpeed() != 0.25f)
                ghost.setSpeed(0.25f);
            else if (slowFactor == 0.5f && ghost.getSpeed() != 0.5f)
                ghost.setSpeed(0.5f);
            else if (slowFactor == 0.75f && ghost.getSpeed() != 0.75f)
                ghost.setSpeed(0.75f);
            else if (slowFactor == 1f && ghost.getSpeed() != 1f)
                ghost.setSpeed(1f);
        }

        if (minValue <= 2f)
            return true;
        return false;
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public void setPowerLevel(int powerLevel) {
        this.powerLevel = powerLevel;
    }

}
