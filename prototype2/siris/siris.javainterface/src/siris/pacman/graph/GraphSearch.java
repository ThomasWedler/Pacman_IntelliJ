package siris.pacman.graph;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/8/12
 * Time: 9:18 AM
 */

/**
 * A graph search.
 */
public interface GraphSearch {

    /**
     * Searches a node in the graph (given by startNode) that satisfies goalTest.
     *
     * @param startNode A node of the graph that shall be searched.
     * @param goalTest A function that test if a node is the goal.
     * @return A node that satisfies goalTest or null if no node was found.
     */
    public Node search(Node startNode, GoalTestFunction goalTest);
}
