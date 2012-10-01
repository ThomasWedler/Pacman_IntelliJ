package siris.pacman.graph;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 10:45 PM
 */

/**
 * Represents a ghost in the context of the pacman application.
 */
public interface Ghost extends MovingEntityNode {

    /**
     * Returns the ghosts type. This is for example used to determine the color of the ghost.
     * @return The ghosts type.
     */
    public int getNr();
}
