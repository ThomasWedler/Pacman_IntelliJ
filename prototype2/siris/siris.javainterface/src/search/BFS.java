package search;

import java.util.LinkedList;

import siris.pacman.graph.GoalTestFunction;
import basic.MyNode;

public class BFS {

	private LinkedList<MyNode> visited = new LinkedList<MyNode>();
	private LinkedList<MyNode> qFringe = new LinkedList<MyNode>();
	private MyNode result;

    /**
     * Breadth-First Search call
     * @param n Start node of search
     * @param goalTest Goal/target of search
     */
	public BFS(MyNode n, GoalTestFunction goalTest) {
        // Set result  in getResult()
		result = recursiveBFS(n, goalTest);
	}

    /**
     * Breath-First Search
     * @param n Start node of search
     * @param goalTest Goal/target of search
     * @return MyNode if n matches goalTest, null if not found at all
     */
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

    /**
     * Breath-First Search result
     * @return MyNode or null
     */
	public MyNode getResult() {
		return result;
	}

}
