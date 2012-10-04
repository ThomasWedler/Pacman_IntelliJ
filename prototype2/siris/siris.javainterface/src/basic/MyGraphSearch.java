package basic;

import search.BFS;
import search.DFS;
import search.DLS;
import search.IDS;
import siris.pacman.graph.GoalTestFunction;
import siris.pacman.graph.Node;

public class MyGraphSearch implements siris.pacman.graph.GraphSearch {

    /* set search to
       - "BFS" for Breadth-First Search
       - "DFS" for Depth-First Search
       - "DLS" for Depth-Limited Search
       - "IDS" for Iterative Deepening Search
     */
	private String search = "DFS";

	private MyNode result = null;

    // Limiter for Depth-Limited Search (@see search.DLS)
    private int dlsLimit = 2;

	@Override
	public Node search(Node startNode, GoalTestFunction goalTest) {
		if (search.equals("BFS"))
			result = new BFS((MyNode) startNode, goalTest).getResult();
		else if (search.equals("DFS"))
			result = new DFS((MyNode) startNode, goalTest).getResult();
		else if (search.equals("DLS"))
			result = new DLS((MyNode) startNode, goalTest, dlsLimit).getResult();
		else if (search.equals("IDS"))
			result = new IDS((MyNode) startNode, goalTest).getResult();

		return result;
	}

}
