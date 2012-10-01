package siris.pacman.graph;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 7:20 PM
 */

/**
 * Represents a moving entity in the context of the pacman application.
 */
public interface MovingEntityNode extends EntityNode {

    /**
     * Sets the desired movement direction of this entity.
     * @param x The x value of the desired movement direction.
     * @param y The y value of the desired movement direction.
     */
    public void setDesiredMovementDirection(int x, int y);

    /**
     * Returns the x value of the desired movement direction.
     * @return the x value of the desired movement direction.
     */
    public int getDesiredMovementDirectionX();

    /**
     * Returns the y value of the desired movement direction.
     * @return the y value of the desired movement direction.
     */
    public int getDesiredMovementDirectionY();

    /**
     * Sets the current movement direction of this entity.
     * @param x The x value of the current movement direction.
     * @param y The y value of the current movement direction.
     */
    public void setCurrentMovementDirection(int x, int y);

    /**
     * Returns the x value of the current movement direction.
     * @return the x value of the current movement direction.
     */
    public int getCurrentMovementDirectionX();

    /**
     * Returns the y value of the current movement direction.
     * @return the y value of the current movement direction.
     */
    public int getCurrentMovementDirectionY();

    /**
     * Returns the speed of this entity. Reasonable values are [0.2f and 5f]
     * @return the speed of this entity.
     */
    public float getSpeed();

}
