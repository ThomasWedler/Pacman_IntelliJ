package basic;
import java.util.LinkedList;
import java.util.UUID;

public class MyNode implements siris.pacman.graph.Node {

	private UUID id = UUID.randomUUID();

    private MyPacman pacman;
    private MyLevel level;

	private LinkedList<MyNode> neighbors = new LinkedList<MyNode>();
	private float g = 0;
	private float f = 0;

	public void connectTo(MyNode n) {
		if (!neighbors.contains(n))
			neighbors.add(n);
		if (!n.neighbors.contains(this))
			n.neighbors.add(this);
	}

	@Override
	public MyNode[] neighbors() {
		MyNode[] array = new MyNode[neighbors.size()];
		MyNode[] neighbours = neighbors.toArray(array);
		return neighbours;
	}

    @Override
    public UUID id() {
        return id;
    }

    public MyPacman getPacman() {
        return pacman;
    }

    public void setPacman(MyPacman pacman) {
        this.pacman = pacman;
    }

    public MyLevel getLevel() {
        return level;
    }

    public void setLevel(MyLevel level) {
        this.level = level;
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

}
