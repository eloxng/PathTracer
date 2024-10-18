package net.runelite.client.plugins.pathtracer;

import net.runelite.api.coords.LocalPoint;

public class TileNode {
    //int x,y;
    LocalPoint localPoint;
    int gcost, hcost, fcost;
    TileNode parent;

    // Constructor to set a local point to a TileNode
    TileNode(LocalPoint localPoint){
        this.localPoint = localPoint;
    }

    // Get and set the Scene x and y coordinates of the LocalPoint
    //public int getX() { return x; }
    //public void setX(LocalPoint tile) { this.x = tile.getSceneX(); }
    //public int getY() { return y; }
    //public void setY(LocalPoint tile) { this.y = tile.getSceneX(); }
    public LocalPoint getLocalPoint(){ return localPoint; }

    // Getting and setting costs for A star algorithm
    public int getGcost() { return gcost; }
    public void setGcost(int gcost) { this.gcost = gcost; }
    public int getHcost() { return hcost; }
    public void setHcost(int hcost) { this.hcost = hcost; }
    public int getFcost() { return fcost; }
    public void setFcost(int fcost) { this.fcost = fcost; }
    public void setCosts(int gcost, int hcost, int fcost){
        this.gcost = gcost;
        this.hcost = hcost;
        this.fcost = hcost;
    }
    // Sets and gets parent of TileNode object
    public TileNode getParent() { return parent; }
    public void setParent(TileNode parent) { this.parent = parent; }
}
