package siris.pacman.graph;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/8/12
 * Time: 9:18 AM
 */

/**
 * A goal test function for a graph search.
 */
public interface GoalTestFunction {

    /**
     * Test if a node satisfies the goal requirements of a graph search.
     * @param n The node to test.
     * @return True if the node satisfies the goal requirements of a graph search, otherwise false.
     */
    public boolean testGoal(Node n);
}
