package basic;

import siris.pacman.BasicPacman;

public class MyMovingEntityNode extends MyEntityNode implements siris.pacman.graph.MovingEntityNode {

	private int cmdX = 0;
	private int cmdY = 0;
	private int dmdX = 0;
	private int dmdY = 0;
	private float speed = 1f;
	private String direction = "none";

	@Override
	public int getCurrentMovementDirectionX() {
		return cmdX;
	}

	@Override
	public int getCurrentMovementDirectionY() {
		return cmdY;
	}

	@Override
	public int getDesiredMovementDirectionX() {
		return dmdX;
	}

	@Override
	public int getDesiredMovementDirectionY() {
		return dmdY;
	}

	@Override
	public float getSpeed() {
		return speed;
	}
	
	public void setSpeed(float speed) {
		this.speed = speed;
	}
	
	String getDirection() {
		return direction;
	}

    /**
     * Set current movement direction of ghosts and Pacman (and rotate them in according to direction).
     * @param x The x value of the current movement direction.
     * @param y The y value of the current movement direction.
     */
	@Override
	public void setCurrentMovementDirection(int x, int y) {
		if (x > 0) {
			direction = "right";
            BasicPacman.rotateEntityTo(id(), -80f);
		}
		if (x < 0) {
			direction = "left";
            BasicPacman.rotateEntityTo(id(), 80f);
        }
		if (y > 0) {
			direction = "up";
            BasicPacman.rotateEntityTo(id(), 160.25f);
        }
		if (y < 0) {
			direction = "down";
            BasicPacman.rotateEntityTo(id(), 0f);
        }
		cmdX = x;
		cmdY = y;
	}

	@Override
	public void setDesiredMovementDirection(int x, int y) {
		dmdX = x;
		dmdY = y;
	}


}
