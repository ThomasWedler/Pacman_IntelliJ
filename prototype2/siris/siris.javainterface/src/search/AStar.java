package search;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;
import basic.MyEntityNode;
import basic.MyNode;
import basic.MyTileNode;

public class AStar {

	private HashSet<MyTileNode> closedSet = new HashSet<MyTileNode>();
	private PriorityQueue<MyTileNode> openSet = new PriorityQueue<MyTileNode>(10, new NodeComparator());
	private HashMap<MyTileNode, MyTileNode> cameFrom = new HashMap<MyTileNode, MyTileNode>();
	private LinkedList<MyTileNode> result = new LinkedList<MyTileNode>();
	private float tentativeG = 0;

	public AStar(MyEntityNode e1, MyEntityNode e2) {
		MyTileNode start = e1.getTileNode();
		MyTileNode goal = e2.getTileNode();
		start.setG(0);
		start.setF(start.getG() + h(start, goal));
		result = compute(start, goal);
		result.add(goal);
	}
	
	public LinkedList<MyTileNode> getResult() {
		return result;
	}

	private LinkedList<MyTileNode> compute(MyTileNode start, MyTileNode goal) {
        openSet.add(start);
        while (!openSet.isEmpty()) {
			MyTileNode current = openSet.poll();

			if (current.equals(goal))
				return reconstructPath(cameFrom, goal);

			closedSet.add(current);
			for (MyNode n : current.getNeighbors()) {
				if (n instanceof MyTileNode) {
					MyTileNode neighbour = (MyTileNode) n;
					if (closedSet.contains(neighbour)) {
						continue;
					}
					tentativeG = current.getG() + 1f;
					if (!openSet.contains(neighbour) || tentativeG < neighbour.getG()) {
						if (!openSet.contains(neighbour)) {
							openSet.add(neighbour);
						}
						cameFrom.put(neighbour, current);
						neighbour.setG(tentativeG);
						neighbour.setF(neighbour.getG() + h(neighbour, goal));
					}
				}
			}
		}
		return null;
	}

	private LinkedList<MyTileNode> reconstructPath(HashMap<MyTileNode, MyTileNode> cameFrom, MyTileNode node) {
		if (cameFrom.containsKey(node)) {
			LinkedList<MyTileNode> path = reconstructPath(cameFrom, cameFrom.get(node));
			path.add(node);
			return path;
		} else {
			result.add(node);
			return result;
		}
	}

	private float h(MyTileNode n1, MyTileNode n2) {
		float a, b, c;
		float diffX, diffY;
		diffX = n1.position().x() - n2.position().x();
		diffY = n1.position().y() - n2.position().y();
		a = Math.abs(diffX);
		b = Math.abs(diffY);
		c = (float) (Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2)));
		return c;
	}

	private class NodeComparator implements Comparator<MyTileNode> {
		public int compare(MyTileNode n1, MyTileNode n2) {
			float f1 = n1.getF();
			float f2 = n2.getF();
			if (f1 < f2)
				return -1;
			if (f1 > f2)
				return 1;
			return 0;
		}
	}

}
