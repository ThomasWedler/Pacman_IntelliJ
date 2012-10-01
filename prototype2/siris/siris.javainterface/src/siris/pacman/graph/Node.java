package siris.pacman.graph;

import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 1:10 PM
 */

/**
 * The base interface for all nodes of the pacman graph
 */
public interface Node {

    /**
     * Returns a universally unique identifier (UUID)
     * Note: Use UUID.randomUUID() to generate a UUID for a node.
     * @return a universally unique identifier (UUID)
     */
    public UUID id();

    /**
     * Returns all neighbor nodes.
     * @return all neighbor nodes.
     */
    public Node[] neighbors();
}
