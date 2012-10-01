package basic;

public class MyPacman extends MyMovingEntityNode implements siris.pacman.graph.Pacman{

	private int powerLevel = 1;

	public int getPowerLevel() {
		return powerLevel;
	}

	public void setPowerLevel(int powerLevel) {
		this.powerLevel = powerLevel;
	}
	
}
