package siris.pacman.util;

import simplex3d.math.intm.Vec2i;
import simplex3d.math.intm.Vec2i$;

/**
 * Created by IntelliJ IDEA.
 * User: martin
 * Date: 5/7/12
 * Time: 1:46 PM
 */

/**
 * A 2D tile position.
 */
public class TilePosition {

    private int _x, _y;

    public TilePosition(int x, int y){_x=x;_y=y;}

    public int x() {return _x;}
    public int y() {return _y;}

    public Vec2i toVec() {return Vec2i$.MODULE$.apply(_x,_y);}
}
