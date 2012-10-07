package basic;

import siris.pacman.BasicPacman;

import java.util.LinkedList;

public class MyPacman extends MyMovingEntityNode implements siris.pacman.graph.Pacman {

    private int powerLevel = 1;
    private boolean isPoweredUp = false;

    private float powerUpTimer = 0f;

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

        if (slowFactor == 0.1f)
            setSpeed(0.1f);
        else if (slowFactor == 0.25f)
            setSpeed(0.25f);
        else if (slowFactor == 0.5f)
            setSpeed(0.5f);
        else if (slowFactor == 0.75f)
            setSpeed(0.75f);
        else if (slowFactor == 1f)
            setSpeed(1f);

        for (MyGhost ghost : getLevel().getGhosts()) {
            if (slowFactor == 0.1f) {
                if (ghost.seesPacman() || ghost.feelsPacman())
                    ghost.setSpeed(0.1f * 1.2f);
                else if (ghost.hearsPacman())
                    ghost.setSpeed(0.1f * 0.6f);
            } else if (slowFactor == 0.25f) {
                if (ghost.seesPacman() || ghost.feelsPacman())
                    ghost.setSpeed(0.25f * 1.2f);
                else if (ghost.hearsPacman())
                    ghost.setSpeed(0.25f * 0.6f);
            } else if (slowFactor == 0.5f) {
                if (ghost.seesPacman() || ghost.feelsPacman())
                    ghost.setSpeed(0.5f * 1.2f);
                else if (ghost.hearsPacman())
                    ghost.setSpeed(0.5f * 0.6f);
            } else if (slowFactor == 0.75f) {
                if (ghost.seesPacman() || ghost.feelsPacman())
                    ghost.setSpeed(0.75f * 1.2f);
                else if (ghost.hearsPacman())
                    ghost.setSpeed(0.75f * 0.6f);
            } else if (slowFactor == 1f) {
                if (ghost.seesPacman() || ghost.feelsPacman())
                    ghost.setSpeed(1f * 1.2f);
                else if (ghost.hearsPacman())
                    ghost.setSpeed(1f * 0.6f);
            }
        }

        return minValue <= 2f;
    }

    public void checkForPowerUp(float deltaT) {
        if (isPoweredUp)
            powerUpTimer += deltaT;
        if (powerUpTimer > 15f) {
            powerUpTimer = 0f;
            isPoweredUp = false;
            BasicPacman.setColorToNormal();
            for (MyGhost ghost : getLevel().getGhosts()) {
                ghost.setDesiredPath(new LinkedList<MyTileNode>());
            }
        }
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public void setPowerLevel(int powerLevel) {
        this.powerLevel = powerLevel;
    }

    public boolean isPoweredUp() {
        return isPoweredUp;
    }

    public void setPoweredUp(boolean poweredUp) {
        isPoweredUp = poweredUp;
    }

    public void setPowerUpTimer(float powerUpTimer) {
        this.powerUpTimer = powerUpTimer;
    }

}
