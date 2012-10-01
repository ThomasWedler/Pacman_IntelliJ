package siris.pacman.graph;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 6:51 PM
 */

/**
 * A node that represents an entity in the context of the pacman application.
 */
public interface EntityNode extends Node {

    /**
     * Sets the position of this entity.
     * @param x The x value of the position.
     * @param y The y value of the position.
     */
    public void setPosition(float x, float y);

    /**
     * Returns the x value of the position.
     * @return the x value of the position.
     */
    public float getPositionX();

    /**
     * Returns the y value of the position.
     * @return the y value of the position.
     */
    public float getPositionY();

    /**
     * Returns the corresponding TileNode, the EntityNode is associated with.
     * @return the corresponding TileNode, the EntityNode is associated with or null after disconnect.
     */
    public TileNode getTileNode();

    /**
     * Sets the corresponding TileNode, the EntityNode is associated with.
     * @param n the new corresponding TileNode.
     */
    public void setTileNode(TileNode n);

    /**
     * Disconnects the EntityNode from its corresponding TileNode.
     */
    public void disconnect();

}
