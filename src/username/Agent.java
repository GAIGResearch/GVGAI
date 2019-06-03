package username;

import core.game.GameDescription;
import core.generator.AbstractLevelGenerator;
import tools.ElapsedCpuTimer;
import tools.GameAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Agent that generates a level from a GameDescription.
 */
public class Agent extends AbstractLevelGenerator {

    /**
     * GameDescription object holding all game information.
     */
    private final GameDescription gameDescription;

    /**
     * GameAnalyzer object which adds some tools for extracting information from the gameDescription object.
     */
    private final GameAnalyzer gameAnalyzer;

    /**
     * The level we are generating.
     */
    private Level level;

    /**
     * Count down until level generation is due.
     */
    private final ElapsedCpuTimer elapsedCpuTimer;

    /**
     * Random number generator.
     */
    private final Random rng = new Random();

    /**
     * The x position of the player avatar.
     */
    private int avatarX;

    /**
     * The y position of the player avatar.
     */
    private int avatarY;

    /**
     * Constructor for the level generator.
     * @param game GameDescription object holding all game information.
     * @param elapsedTimer Count down until level generation is due.
     */
    public Agent(GameDescription game, ElapsedCpuTimer elapsedTimer) {
        gameDescription = game;
        gameAnalyzer = new GameAnalyzer(game);
        elapsedCpuTimer = elapsedTimer;
    }

    /**
     * Determines the width and height of the leven and initializes a new Level object.
     * @see Level
     */
    private void initializeLevel() {
        // Determine if we need to add clearance to add a border
        int borderClearance = 2;
        if (gameAnalyzer.getSolidSprites().isEmpty()) borderClearance = 0;

        // Determine the width and height
        int width = Utils.clamp(Constants.MIN_WIDTH,
                (int) (gameDescription.getAllSpriteData().size() * (1 + Constants.RANDOM_WIDTH * rng.nextDouble())),
                Constants.MAX_WIDTH) + borderClearance;
        int height = Utils.clamp(Constants.MIN_HEIGHT,
                (int) (gameDescription.getAllSpriteData().size() * (1 + Constants.RANDOM_HEIGHT * rng.nextDouble())),
                Constants.MAX_HEIGHT) + borderClearance;

        // Initialize the level
        level = new Level(width, height);
    }

    /**
     * Adds a border of random solid sprites around the level.
     */
    private void addSolidBorder() {
        // Get the sprite to use for the border
        List<String> solidSprites = gameAnalyzer.getSolidSprites();
        String solidSprite = solidSprites.get(rng.nextInt(solidSprites.size()));

        // Add a border along the top and bottom side of the level
        for (int x = 0; x < level.getWidth(); x++) {
            level.setSprite(x, 0, solidSprite);
            level.setSprite(x, level.getHeight() - 1, solidSprite);
        }

        // Add a border along the left and right side of the level
        for (int y = 1; y < level.getHeight() - 1; y++) {
            level.setSprite(0, y, solidSprite);
            level.setSprite(level.getWidth() - 1, y, solidSprite);
        }
    }

    /**
     * Places the player avatar into the level.
     */
    private void addPlayerAvatar() {
        // Determine if we need to add clearance to not add the player avatar inside the border
        int borderClearance = 1;
        if (gameAnalyzer.getSolidSprites().isEmpty()) borderClearance = 0;

        // Pick a random location to add the player avatar
        avatarX = rng.nextInt(level.getWidth() - borderClearance) + borderClearance;
        avatarY = rng.nextInt(level.getHeight() - borderClearance) + borderClearance;

        // Add the player avatar
        level.addSprite(avatarX, avatarY, gameAnalyzer.getAvatarSprites().get(0));

        // Temporary, to prevent the game from finishing immediately
        level.addSprite(6, 6, gameAnalyzer.getGoalSprites().get(0));
    }

    /**
     * Fills every space that has not been assigned a spite yet, with a floor sprite if there exists such a sprite.
     */
    private void fillEmptySpaceWithFloor() {
        gameDescription.getStatic().stream().filter(spriteData -> "Immovable".equals(spriteData.type)
                && !spriteData.isSingleton && !spriteData.isResource && !spriteData.isNPC && !spriteData.isAvatar
                && !spriteData.isPortal && gameAnalyzer.getOtherSprites().contains(spriteData.name)
        ).findAny().ifPresent(spriteData -> {
            // If we can find a sprite the looks like a floor tile, add it to all coordinates
            String sprite = spriteData.name;
            level.forEachPosition((x, y) -> level.addSprite(x, y, sprite));
        });
    }

    /**
     * Fills unreachable areas of the level with solid sprites.
     * @param reachable Matrix indicating which positions are reachable, and which are not.
     */
    private void fillUnreachablePositions(boolean[]... reachable) {
        List<String> solidSprites = gameAnalyzer.getSolidSprites();

        // Fill unreachable areas with solid sprites
        String solidSprite = solidSprites.get(rng.nextInt(solidSprites.size()));
        level.forEachPosition((x, y) -> {
            if (!reachable[y][x]) level.setSprite(x, y, solidSprite);
        });
    }

    /**
     * Transforms the level matrix into a single String, and cuts the level to size, using only the relevant parts.
     * @param reachable Matrix indicating which positions are reachable, and which are not.
     * @return The level matrix as a single String.
     */
    private String getLevelCutToSize(boolean[]... reachable) {
        int minX = avatarX;
        int maxX = avatarX;
        int minY = avatarY;
        int maxY = avatarY;

        for (int x = 0; x < level.getWidth(); x++) {
            for (int y = 0; y < level.getHeight(); y++) {
                if (reachable[y][x]) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        return level.getLevel(minX, maxX + 1, minY, maxY + 1);
    }

    /**
     * Finds positions which are reachable from the given position.
     * @param reachable Matrix of booleans, indicating which positions are reachable, and which are not.
     * @param x The x position we are trying to reach other positions from.
     * @param y The y position we are trying to reach other positions from.
     * @param sprites The list of solid sprites.
     */
    private void findReachablePositions(final boolean[][] reachable, int x, int y, List<String> sprites) {
        // If this coordinate has been checked before, do nothing
        if (reachable[y][x]) return;
        reachable[y][x] = true;

        // If this coordinate contains a solid sprite, then neighbouring sprites won't be reachable from here
        if (sprites.stream().anyMatch(level.get(x, y)::contains)) return;

        // Get coordinates
        int[] xCoordinates = {x - 1, x, x + 1, x};
        int[] yCoordinates = {y, y + 1, y, y - 1};

        for (int i = 0; i < xCoordinates.length; i++) {
            // Check x coordinate is within bounds
            int xCoordinate = xCoordinates[i];
            if (xCoordinate < 0 || xCoordinate >= level.getWidth()) continue;

            // Check y coordinate is within bounds
            int yCoordinate = yCoordinates[i];
            if (yCoordinate < 0 || yCoordinate >= level.getHeight()) continue;

            // Check the the coordinate
            findReachablePositions(reachable, xCoordinate, yCoordinate, sprites);
        }
    }

    @Override
    public String generateLevel(GameDescription game, ElapsedCpuTimer elapsedTimer) {
        List<String> solidSprites = gameAnalyzer.getSolidSprites();
        boolean solidSpritesExist = !solidSprites.isEmpty();

        initializeLevel();

        if (solidSpritesExist) addSolidBorder();

        addPlayerAvatar();
        fillEmptySpaceWithFloor();

        if (solidSpritesExist) {
            // Check which areas are reachable
            boolean[][] reachable = new boolean[level.getHeight()][level.getWidth()];
            findReachablePositions(reachable, avatarX, avatarY, solidSprites);

            fillUnreachablePositions(reachable);

            return getLevelCutToSize(reachable);
        }

        return level.getLevel();
    }

    @Override
    public HashMap<Character, ArrayList<String>> getLevelMapping() {
        return Level.getLEVEL_MAPPING().getCharMapping();
    }
}