package search;

import java.util.LinkedList;
import java.util.Stack;

import siris.pacman.graph.GoalTestFunction;
import basic.MyNode;

public class DFS {

	private LinkedList<MyNode> visited = new LinkedList<MyNode>();
	private Stack<MyNode> sFringe = new Stack<MyNode>();
	private MyNode result;

	public DFS(MyNode n, GoalTestFunction goalTest) {
		result = recursiveDFS(n, goalTest);
	}

	// Depth-First-Search
	private MyNode recursiveDFS(MyNode n, GoalTestFunction goalTest) {
		if (goalTest.testGoal(n)) {
			return n;
		}
		visited.add(n);
		for (MyNode node : n.neighbors()) {
			if (!visited.contains(node)) {
				sFringe.addElement(node);
				visited.add(node);
			}
		}
		if (!sFringe.isEmpty()) {
			recursiveDFS(sFringe.pop(), goalTest);
		}
		return null;
	}

	public MyNode getResult() {
		return result;
	}
}
