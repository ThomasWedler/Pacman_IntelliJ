package basic;

import search.BFS;
import search.DFS;
import search.DLS;
import search.IDS;
import siris.pacman.graph.GoalTestFunction;
import siris.pacman.graph.Node;

public class MyGraphSearch implements siris.pacman.graph.GraphSearch {

	private String search = "DFS";
	private MyNode result = null;

	@Override
	public Node search(Node startNode, GoalTestFunction goalTest) {
		if (search.equals("BFS"))
			result = new BFS((MyNode) startNode, goalTest).getResult();
		else if (search.equals("DFS"))
			result = new DFS((MyNode) startNode, goalTest).getResult();
		else if (search.equals("DLS"))
			result = new DLS((MyNode) startNode, goalTest, 2).getResult();
		else if (search.equals("IDS"))
			result = new IDS((MyNode) startNode, goalTest).getResult();
		
		return result;
	}

}
