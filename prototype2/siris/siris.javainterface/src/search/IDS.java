package search;

import siris.pacman.graph.GoalTestFunction;
import basic.MyNode;

public class IDS {

	private MyNode result;

    /**
     * Iterative Deepening Search call
     * @param n Start node of search
     * @param goalTest Goal/target of search
     */
	public IDS(MyNode n, GoalTestFunction goalTest) {
		result = recursiveIDS(n, goalTest);
	}

    /**
     * Iterative Deepening Search
     * @param n Start node of search
     * @param goalTest Goal/target of search
     * @return MyNode if n matches goalTest, null if not found at all
     */
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

    /**
     * Iterative Deepening Search result
     * @return MyNode or null
     */
	public MyNode getResult() {
		return result;
	}
}
