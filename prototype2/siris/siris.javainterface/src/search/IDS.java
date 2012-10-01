package search;

import siris.pacman.graph.GoalTestFunction;
import basic.MyNode;

public class IDS {

	private MyNode result;

	public IDS(MyNode n, GoalTestFunction goalTest) {
		result = recursiveIDS(n, goalTest);
	}

	// Iterative-Deepening-Search
	private MyNode recursiveIDS(MyNode n, GoalTestFunction goalTest) {
		for (int depth = 0;; depth++) {
			DLS dls = new DLS(n, goalTest, depth);
			MyNode node = dls.getResult();
			MyNode cutoff = dls.getCutoff();
			if (node != cutoff) {
				return node;
			}
			if (node == null)
				return null;
		}
	}

	public MyNode getResult() {
		return result;
	}
}
