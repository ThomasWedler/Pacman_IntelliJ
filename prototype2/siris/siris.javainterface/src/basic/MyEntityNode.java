package basic;

import siris.pacman.graph.TileNode;

public class MyEntityNode extends MyNode implements
		siris.pacman.graph.EntityNode {

	private float x;
	private float y;
	private TileNode tileNode;

	@Override
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public float getPositionX() {
		return x;
	}

	@Override
	public float getPositionY() {
		return y;
	}

	@Override
	public TileNode getTileNode() {
		return tileNode;
	}

	@Override
	public void setTileNode(TileNode n) {
		this.disconnect();
		this.connectTo((MyNode) n);
		this.tileNode = n;
	}

	@Override
	public void disconnect() {
		MyNode n = null;
		if (this.getNeighbors().size() != 0) {
			n = this.getNeighbors().getFirst();
			this.getNeighbors().remove(n);
			n.getNeighbors().remove(this);
		}
	}

}
