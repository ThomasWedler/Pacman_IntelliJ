package search;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Stack;

import siris.pacman.graph.GoalTestFunction;
import basic.MyNode;

public class DLS {

	private LinkedList<MyNode> visited = new LinkedList<MyNode>();
	private Stack<MyNode> sFringe = new Stack<MyNode>();
	private Map<MyNode, Integer> depthMap = new HashMap<MyNode, Integer>();
	private MyNode cutoff = null;
	private MyNode result;
	private int depth = 0;

    /**
     * Depth-Limited Search call
     * @param n Start node of search
     * @param goalTest Goal/target of search
     * @param limit Limiter for cutoff
     */
	public DLS(MyNode n, GoalTestFunction goalTest, int limit) {
		result = recursiveDLS(n, goalTest, limit);
	}

    /**
     * Depth-Limited Search
     * @param n Start node of search
     * @param goalTest Goal/target of search
     * @param limit Limiter for cutoff
     * @return MyNode if n matches goalTest or if limit is reached (cutoff) or null
     */
	private MyNode recursiveDLS(MyNode n, GoalTestFunction goalTest, int limit) {
		boolean cutoffocc = false;
		visited.add(n);
		if (!depthMap.containsKey(n))
			depthMap.put(n, depth);
		depth = depthMap.get(n) + 1;
		if (goalTest.testGoal(n)) {
			return n;
		} else if (depthMap.get(n) == limit) {
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

    /**
     * Depth-Limited Search result
     * @return MyNode or null
     */
	public MyNode getResult() {
		return result;
	}

    /**
     * Depth-Limited Search cutoff
     * @return MyNode of cutoff occurs
     */
    public MyNode getCutoff() {
		return cutoff;
	}

}
