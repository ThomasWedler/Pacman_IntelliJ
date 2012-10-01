package siris.pacman.graph;

import siris.pacman.util.TilePosition;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 1:45 PM
 */

/**
 * A Node that represents a tile of the pacman level.
 */
public interface TileNode extends Node {

    /**
     * Returns the position of the level tile.
     * @return the position of the level tile.
     */
    public TilePosition position();
}
