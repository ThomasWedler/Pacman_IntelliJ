package search;

import java.util.LinkedList;

import siris.pacman.graph.GoalTestFunction;
import basic.MyNode;

public class BFS {

	private LinkedList<MyNode> visited = new LinkedList<MyNode>();
	private LinkedList<MyNode> qFringe = new LinkedList<MyNode>();
	private MyNode result;

	public BFS(MyNode n, GoalTestFunction goalTest) {
		result = recursiveBFS(n, goalTest);
	}

	// Breadth-First-Search
	private MyNode recursiveBFS(MyNode n, GoalTestFunction goalTest) {
		if (goalTest.testGoal(n)) {
			return n;
		}
		visited.add(n);
		for (MyNode node : n.neighbors()) {
			if (!visited.contains(node)) {
				qFringe.add(node);
				visited.add(node);
			}
		}
		if (!qFringe.isEmpty()) {
			recursiveBFS(qFringe.poll(), goalTest);
		}
		return null;
	}
	
	public MyNode getResult() {
		return result;
	}

}
