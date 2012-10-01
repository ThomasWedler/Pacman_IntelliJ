package basic;

import pacman.Game;

public class MyMovingEntityNode extends MyEntityNode implements siris.pacman.graph.MovingEntityNode {

	private int cmdX = 0;
	private int cmdY = 0;
	private int dmdX = 0;
	private int dmdY = 0;
	private float speed = 3f * Game.gameSpeed;
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
	
	public String getDirection() {
		return direction;
	}

	@Override
	public void setCurrentMovementDirection(int x, int y) {
		if (x > 0) {
			direction = "right";
		}
		if (x < 0) {
			direction = "left";
		}
		if (y > 0) {
			direction = "up";
		}
		if (y < 0) {
			direction = "down";
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
