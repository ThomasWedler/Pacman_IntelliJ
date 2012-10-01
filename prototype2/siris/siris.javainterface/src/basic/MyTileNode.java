package basic;

import siris.pacman.util.TilePosition;

public class MyTileNode extends MyNode implements siris.pacman.graph.TileNode {
	
	private TilePosition position;
	
	@Override
	public TilePosition position() {
		return position;
	}
	
	public void setPosition(TilePosition p) {
		this.position = p;
	}
	
	public String getDifferenceBetweenPositions(MyTileNode n) {
		int diffX = this.position().x() - n.position().x();
		int diffY = this.position().y() - n.position().y();
		if (diffX != 0) {
			if (diffX > 0)
				return "left";
			if (diffX < 0)
				return "right";
		}
		if (diffY != 0) {
			if (diffY > 0)
				return "down";
			if (diffY < 0)
				return "up";
		}
		return "none";
	}

}
