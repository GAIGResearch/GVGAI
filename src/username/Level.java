package username;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Class representing a level, containing a matrix, containing chars which represent sprites.
 */
public class Level {

    /**
     * The LEVEL_MAPPING for this level.
     */
    @Getter private static final LevelMapping LEVEL_MAPPING = new LevelMapping();

    /**
     * The level we are generating, represented as a matrix, containing chars which represent sprites.
     * The outer array contains the rows, i.e. y coordinate.
     * The inner array contain the columns, i.e. x coordinate.
     */
    private final char[][] matrix;

    /**
     * The width of the matrix.
     */
    @Getter private int width;

    /**
     * The height of the matrix.
     */
    @Getter private int height;

    /**
     * Constructs a new level, of the specified size.
     * @param width The width of the matrix.
     * @param height The height of the matrix.
     */
    Level(int width, int height) {
        this.width = width;
        this.height = height;

        // Initialize the level matrix
        char emtpyChar = LEVEL_MAPPING.get();
        matrix = new char[height][width];
        for (char[] column : matrix) {
            Arrays.fill(column, emtpyChar);
        }
    }

    /**
     * Returns the list of sprites for a given coordinate.
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return The list of sprites for a given coordinate.
     */
    List<String> get(int x, int y) {
        return LEVEL_MAPPING.get(matrix[y][x]);
    }

    /**
     * Adds the sprite at the specified location, in the level matrix.
     * @param x The x coordinate of where to set the sprite.
     * @param y The y coordinate of where to set the sprite.
     * @param sprite The sprite to set at the specified coordinates.
     */
    void addSprite(int x, int y, String sprite) {
        matrix[y][x] = LEVEL_MAPPING.getWith(matrix[y][x], sprite);
    }

    /**
     * Sets the sprite at the specified location, in the level matrix.
     * @param x The x coordinate of where to set the sprite.
     * @param y The y coordinate of where to set the sprite.
     * @param sprite The sprite to set at the specified coordinates.
     */
    void setSprite(int x, int y, String sprite) {
        // TODO add behaviour for cut of areas of the level (for example: fill with solid sprites)
        matrix[y][x] = LEVEL_MAPPING.get(sprite);
    }

    /**
     * Removes the sprite from the specified location, in the level matrix.
     * @param x The x coordinate of where to set the sprite.
     * @param y The y coordinate of where to set the sprite.
     * @param sprite The sprite to remove from the specified coordinates.
     */
    void removeSprite(int x, int y, String sprite) {
        matrix[y][x] = LEVEL_MAPPING.getWithout(matrix[y][x], sprite);
    }

    /**
     * Moves the sprite to a new location, in the level matrix.
     * If the sprite does not exist at the old location, it will still be added to the new location.
     * @param xOld The x coordinate of where the sprite is now.
     * @param yOld The y coordinate of where the sprite is now.
     * @param xNew The x coordinate of where the sprite should be moved to.
     * @param yNew The y coordinate of where the sprite should be moved to.
     * @param sprite The sprite to be moved.
     */
    void moveSprite(int xOld, int yOld, int xNew, int yNew, String sprite) {
        removeSprite(xOld, yOld, sprite);
        addSprite(xNew, yNew, sprite);
    }

    /**
     * Applies a function that takes two integers (x and y position) to each position in the level.
     * @param fun A function that takes an x and y position as an input.
     */
    void forEachPosition(BiConsumer<Integer, Integer> fun) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                fun.accept(x, y);
            }
        }
    }

    /**
     * Transforms the level matrix into a single String.
     * @return The level matrix as a single String.
     */
    String getLevel() {
        return getLevel(0, width, 0, height);
    }

    /**
     * Transforms the level matrix into a single String, but only within the range of given coordinates.
     * @param minX The minimum x coordinate (inclusive).
     * @param maxX The maximum x coordinate (exclusive).
     * @param minY The minimum y coordinate (inclusive).
     * @param maxY The maximum y coordinate (exclusive).
     * @return The level matrix as a single String.
     */
    String getLevel(int minX, int maxX, int minY, int maxY) {
        StringBuilder result = new StringBuilder();
        for (int y = minY; y < maxY; y++) {
            result.append(String.valueOf(matrix[y], minX, maxX - minX)).append('\n');
        }
        return result.substring(0, result.length() - 1);
    }
}
