package siris.pacman;


import siris.pacman.graph.EntityNode;
import siris.pacman.graph.MovingEntityNode;
import siris.pacman.graph.Node;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 1:01 PM
 */

/**
 * Handlers for the basic pacman application that allow the implementation of ai operations.
 */
public interface PacmanAI {

    /**
     * Called at the beginning of every simulation step
     * @param deltaT The time passed since the last simulation step in seconds.
     */
    public void onSimulationStep(float deltaT);

    /**
     * Called if a new desired direction has to be inferred for a MovingEntityNode.
     * @param entityToDecideFor The MovingEntityNode to decide for.
     */
    public void onDecisionRequired(MovingEntityNode entityToDecideFor);

    /**
     * Called if two entities collided.
     * @param e1 The first colliding entity.
     * @param e2 The second colliding entity.
     */
    public void onCollision(EntityNode e1, EntityNode e2);
}
