package net.runelite.client.plugins.pathtracer;

import net.runelite.api.*;
import net.runelite.api.Menu;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import java.util.*;

import com.google.inject.Provides;

@PluginDescriptor(
        name = "Path Tracer",
        description = "Trace the shortest walking path to your destination",
        tags = {"pathfinder", "maps"}
)
public class PathTracerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private PathTracerOverlay pathTracerOverlay;

    // Offsets used for getNeighbors()
    // ORDER: SW W NW S N SE E NE
    private final int[][] offsets = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},         {0, 1},
            {1, -1}, {1, 0}, {1, 1}
    };
    private final String[] directions = {"SW", "W", "NW",
                                        "S", "N",
                                        "SE", "E", "NE"};


    @Provides
    PathTracerConfig provideConfig(ConfigManager configManager) { return configManager.getConfig(PathTracerConfig.class); }

    @Override
    protected void startUp() throws Exception
    {
        overlayManager.add(pathTracerOverlay);
    }

    @Override
    protected void shutDown() throws Exception
    {
        overlayManager.remove(pathTracerOverlay);
    }


    @Subscribe
    public void onMenuEntryAdded(MenuEntryAdded event){
        // If you're right-clicking an in-game tile...
        if (event.getOption().equals("Walk here"))
            addMenuEntries();
    }
    private void addMenuEntries(){
        // Make a new menu using the client
        Menu menu = client.getMenu();
        // Create menu entries using the new Menu variable
        MenuEntry[] entries = menu.getMenuEntries();
        MenuEntry[] newEntries = new MenuEntry[entries.length + 2];
        System.arraycopy(entries, 0, newEntries, 0, entries.length);
        MenuEntry entry1 = menu.createMenuEntry(entries.length)
                .setOption("Select destination")
                .setTarget("")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> findPath());
        MenuEntry entry2 = menu.createMenuEntry(entries.length)
                .setOption("Clear the path")
                .setTarget("")
                .setType(MenuAction.RUNELITE)
                .onClick(e -> clearPath());
        // Set the menu entries into 'menu'
        newEntries[newEntries.length - 2] = entry2;
        newEntries[newEntries.length - 1] = entry1;
        menu.setMenuEntries(newEntries);
    }

    // Returns destination tile as a LocalPoint
    public LocalPoint getLPDestinationTile() {
        Tile target = client.getTopLevelWorldView().getSelectedSceneTile();
        if(target == null)
            return null;
        return target.getLocalLocation();
    }

    // Returns the current tile as a LocalPoint
    public LocalPoint getLPCurrentTile() {
        if(client.getLocalPlayer() == null)
            return null;
        return client.getLocalPlayer().getLocalLocation();
    }

    // Returns the destination tile as a WorldPoint
    public WorldPoint getWPDestinationTile(){ return WorldPoint.fromLocalInstance(client, getLPDestinationTile()); }

    // Returns the current tile as a WorldPoint
    public WorldPoint getWPCurrentTile(){ return WorldPoint.fromLocalInstance(client, getLPCurrentTile()); }

    // Returns a WorldView
    public WorldView getWorldView(){ return client.getTopLevelWorldView(); }

    // Pass null for all three parameters in drawTiles()
    private void clearPath() {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Clearing path", "");
        pathTracerOverlay.addTilesToDraw(null);
    }

    // Function that gets executed when "Select destination" is pressed
    private void findPath() {
        // Initialize start and goal LocalPoints
        LocalPoint start = getLPCurrentTile();
        LocalPoint goal = getLPDestinationTile();
        // If start/goal is somehow null
        if (start == null || goal == null) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Start and or goal point is null.", "");
            return;
        }
        // If destination is unreachable to begin with
        if(!isTileWalkable(goal)){
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Destination unreachable", "");
            return;
        }
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Start and or goal point initialized.", "");

        // Initialize startTile and goalTile TileNode's
        TileNode startTile = new TileNode(start);
        TileNode goalTile = new TileNode(goal);
        startTile.setGcost(0);
        startTile.setHcost(calculateManhattanDistance(startTile, goalTile));
        startTile.setFcost(startTile.getGcost() + startTile.getHcost());
        goalTile.setHcost(0);

        clientThread.invoke(() -> {
            TileNode path = AStarPath(startTile, goalTile);
            if (path != null) {
                pathTracerOverlay.addTilesToDraw(path);
            }
        });
    }

    private TileNode AStarPath(TileNode start, TileNode goal){
        // Initialize open and closed lists
        PriorityQueue<TileNode> openList = new PriorityQueue<>(Comparator.comparingInt(t -> t.getFcost()));
        Set<TileNode> closedList = new HashSet<>();
        openList.add(start);

        while(!openList.isEmpty()) {
            // Get node with lowest fCost
            TileNode currentTile = openList.poll();
            closedList.add(currentTile);

            // If current tile reaches the destination
            if(currentTile.getLocalPoint().equals(goal.getLocalPoint())){
                client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Path found", "");
                return currentTile;
            }

            // Check the neighboring tiles
            for(TileNode neighbor : getNeighbors(currentTile)){
                if(closedList.contains(neighbor) || !isTileWalkable(neighbor.getLocalPoint()))
                    continue; // Skip neighbor that is closed, not walkable, or blocked by a wall from current to neighbor

                // Calculate tentative g cost
                int tgcost = currentTile.getGcost() + calculateManhattanDistance(currentTile, neighbor);

                if(tgcost < neighbor.getGcost() || !openList.contains(neighbor)) {
                    // Set parent
                    neighbor.setParent(currentTile);
                    // Set the g, h, and f cost of the neighbor
                    neighbor.setGcost(tgcost);
                    neighbor.setHcost(calculateManhattanDistance(neighbor,goal));
                    neighbor.setFcost(neighbor.getGcost() + neighbor.getHcost());

                    // Add neighbor if openList doesn't have it already
                    if(!openList.contains(neighbor))
                        openList.add(neighbor);
                }
            }
        }
        return null;
    }

    // Return a List<TileNode> that has all the current TileNode's neighbors
    private List<TileNode> getNeighbors(TileNode current) {
        List<TileNode> neighbors = new ArrayList<>();
        int sceneX = current.getLocalPoint().getSceneX();
        int sceneY = current.getLocalPoint().getSceneY();

        // Offsets for the 8 possible neighbors
        for(int i = 0; i < offsets.length; i++){
            // Get neighboring scene x and y coordinates
            int nSceneX = sceneX + offsets[i][0];
            int nSceneY = sceneY + offsets[i][1];
            // Make a new local point for the neighboring point
            LocalPoint neighborPoint = LocalPoint.fromScene(nSceneX, nSceneY, getWorldView());
            // ORDER: SW W NW S N SE E NE  0-7

            // Add the neighboring point to neighbors list if that point is walkable AND no wall blocks the current and neighbor
            if(isTileWalkable(neighborPoint) && noWallBlocking(neighborPoint, directions[i]))
                neighbors.add(new TileNode(neighborPoint));
        }
        return neighbors;
    }

    // Helper function to findPath() and AStarPath()
    // Determines distance from one TileNode to another TileNode
    // Used as the h cost for a TileNode
    private int calculateManhattanDistance(TileNode start, TileNode goal) {
        return Math.abs(start.getLocalPoint().getSceneX() - goal.getLocalPoint().getSceneX()) +
                Math.abs(start.getLocalPoint().getSceneY() - goal.getLocalPoint().getSceneY());
    }

    // Determines if an in-game tile is walkable based off the collision map rendered from the user's WorldView
    private boolean isTileWalkable(LocalPoint tile){
        int colFlag; // Collision flag value
        int sceneX = tile.getSceneX(); // X scene coordinates
        int sceneY = tile.getSceneY(); // Y Scene coordinates

        CollisionData[] collisionData = getWorldView().getCollisionMaps();
        if(collisionData == null || collisionData.length == 0 || collisionData[0] == null)
            return false;

        // If not in scene, then not walkable
        if (!tile.isInScene())
            return false;

        // Return based on if colFlag of tile based on if you can move your character to that tile or not
        int[][] colData = collisionData[0].getFlags();
        colFlag = colData[sceneX][sceneY];
        return ((colFlag & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 &&
                (colFlag & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_FULL) == 0);
    }

    // Determines whether a wall is blocking movement in a given direction
    private boolean noWallBlocking(LocalPoint neighborPoint, String direction) {
        int neighborX = neighborPoint.getSceneX();
        int neighborY = neighborPoint.getSceneY();

        // Initialize collision data
        CollisionData[] collisionData = getWorldView().getCollisionMaps();
        if (collisionData == null || collisionData.length == 0 || collisionData[0] == null)
            return false; // Assume there's a wall if no collision data

        int[][] colData = collisionData[0].getFlags();

        // Check wall blocking based on direction
        switch (direction) {
            case "N": // Moving North
                return (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0 &&
                        (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_SOUTH) == 0;
            case "S": // Moving South
                return (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0 &&
                        (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_NORTH) == 0;
            case "E": // Moving East
                return (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0 &&
                        (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_WEST) == 0;
            case "W": // Moving West
                return (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0 &&
                        (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_LINE_OF_SIGHT_EAST) == 0;
            case "NE": // Moving Northeast (combination of N and E)
                return (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0 &&
                        (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
            case "NW": // Moving Northwest (combination of N and W)
                return (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0 &&
                        (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
            case "SE": // Moving Southeast (combination of S and E)
                return (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0 &&
                        (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
            case "SW": // Moving Southwest (combination of S and W)
                return (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0 &&
                        (colData[neighborX][neighborY] & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
            default:
                // If an unrecognized direction is passed, assume there's a wall
                return false;
        }
    }
}

