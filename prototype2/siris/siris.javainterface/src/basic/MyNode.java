package basic;
import java.util.LinkedList;
import java.util.UUID;

public class MyNode implements siris.pacman.graph.Node {

	private UUID id = UUID.randomUUID();
	private LinkedList<MyNode> neighbors = new LinkedList<MyNode>();
	private float g = 0;
	private float f = 0;
	
	public MyNode() {
	}

	public float getG() {
		return g;
	}

	public void setG(float g) {
		this.g = g;
	}

	public float getF() {
		return f;
	}

	public void setF(float f) {
		this.f = f;
	}

	public LinkedList<MyNode> getNeighbors() {
		return neighbors;
	}

	public void connectTo(MyNode n) {
		if (!neighbors.contains(n))
			this.neighbors.add(n);
		if (!n.neighbors.contains(this))
			n.neighbors.add(this);
	}

	@Override
	public UUID id() {
		return id;
	}

	@Override
	public MyNode[] neighbors() {
		MyNode[] array = new MyNode[neighbors.size()];
		MyNode[] neighbours = neighbors.toArray(array);
		return neighbours;
	}

}
