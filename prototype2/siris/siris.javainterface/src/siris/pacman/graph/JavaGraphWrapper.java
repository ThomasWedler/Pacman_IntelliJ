package siris.pacman.graph;

import siris.java.JavaInterface;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/8/12
 * Time: 10:14 AM
 */

/**
 * Java wrapper for the scala class siris.pacman.graph.Graph
 */
public class JavaGraphWrapper {

    /**
     * Uses the JavaInterface to create a graphical representation for the graph given by the Node root.
     * @param root A Node of the graph to be graphically represented.
     * @param inst An instance of JavaInterface.
     */
    public static void drawGraph(Node root, JavaInterface inst) {
        Graph$.MODULE$.drawGraph(root, inst);
    }
}
