package search;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import siris.pacman.graph.GoalTestFunction;
import basic.MyNode;

public class DLS {

	LinkedList<MyNode> visited = new LinkedList<MyNode>();
	private Stack<MyNode> sFringe = new Stack<MyNode>();
	private Map<MyNode, Integer> depthMap = new HashMap<MyNode, Integer>();
	private MyNode cutoff = null;
	private MyNode result;
	private int depth = 0;

	public DLS(MyNode n, GoalTestFunction goalTest, int limit) {
		result = recursiveDLS(n, goalTest, limit);
	}

	// Depth-Limited-Search
	private MyNode recursiveDLS(MyNode n, GoalTestFunction goalTest, int limit) {
		boolean cutoffocc = false;
		visited.add(n);
		if (!depthMap.containsKey(n))
			depthMap.put(n, depth);
		depth = depthMap.get(n).intValue() + 1;
		if (goalTest.testGoal(n)) {
			return n;
		} else if (depthMap.get(n).intValue() == limit) {
			cutoff = n;
			if (!sFringe.isEmpty()) {
				recursiveDLS(sFringe.pop(), goalTest, limit);
			}
			return cutoff;
		} else {
			for (MyNode node : n.neighbors()) {
				if (!visited.contains(node)) {
					sFringe.addElement(node);
					depthMap.put(node, depth);
				}
			}
			if (!sFringe.isEmpty()) {
				MyNode node = recursiveDLS(sFringe.pop(), goalTest, limit);
				if (node != null) {
					if (node.equals(cutoff))
						cutoffocc = true;
					else
						return node;
				}
			}
		}
		if (cutoffocc)
			return cutoff;
		return null;
	}

	public MyNode getResult() {
		return result;
	}
	
	public MyNode getCutoff() {
		return cutoff;
	}

}
