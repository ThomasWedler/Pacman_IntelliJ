package basic;

import siris.pacman.graph.TileNode;

public class MyEntityNode extends MyNode implements siris.pacman.graph.EntityNode {

    private float x;
    private float y;
    private TileNode tileNode;

    @Override
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public float getPositionX() {
        return x;
    }

    @Override
    public float getPositionY() {
        return y;
    }

    @Override
    public MyTileNode getTileNode() {
        return (MyTileNode) tileNode;
    }

    @Override
    public void setTileNode(TileNode tileNode) {
        disconnect();
        connectTo((MyNode) tileNode);
        this.tileNode = tileNode;
    }

    @Override
    public void disconnect() {
        MyNode n;
        if (getNeighbors().size() != 0) {
            n = getNeighbors().getFirst();
            getNeighbors().remove(n);
            n.getNeighbors().remove(this);
        }
    }

}
