package net.runelite.client.plugins.pathtracer;

import net.runelite.api.coords.LocalPoint;

public class TileNode {
    private LocalPoint localPoint;
    private int gcost, hcost, fcost;
    private TileNode parent;

    // Constructor to set a local point to a TileNode
    TileNode(LocalPoint localPoint){
        this.localPoint = localPoint;
    }

    // Get the TileNode's LocalPoint
    public LocalPoint getLocalPoint(){ return localPoint; }

    // Getting and setting costs for A star algorithm
    public int getGcost() { return gcost; }
    public void setGcost(int gcost) { this.gcost = gcost; }
    public int getHcost() { return hcost; }
    public void setHcost(int hcost) { this.hcost = hcost; }
    public int getFcost() { return fcost; }
    public void setFcost(int fcost) { this.fcost = fcost; }

    // Get and set the parent of a TileNode object
    public TileNode getParent() { return parent; }
    public void setParent(TileNode parent) { this.parent = parent; }
}
