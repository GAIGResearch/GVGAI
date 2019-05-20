package username;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Class representing containing a matrix of chars, representing a level.
 */
public class Level {

    /**
     * The level we are generating, where each char is mapped to a string representing a sprite.
     * @see Level#charMapping
     */
    private char[][] level;

    /**
     * The width of the level.
     */
    @Getter private int width;

    /**
     * The height of the level.
     */
    @Getter private int height;

    /**
     * Maps every string representing a sprite, to a char. Opposite of charMapping.
     * @see #charMapping
     */
    private HashMap<String, Character> stringMapping = new HashMap<>();

    /**
     * Maps every char, to a list of strings representing sprites. Opposite of stringMapping.
     * @see #stringMapping
     */
    @Getter private HashMap<Character, ArrayList<String>> charMapping = new HashMap<>();

    /**
     * The next char to use when we need a new char to map to a string in the charMapping and stringMapping.
     * @see #charMapping
     * @see #stringMapping
     */
    private char mappingChar = 'a';

    /**
     * Constructs a new level, of the specified size, and fills it with ' ' chars.
     * @param width The width of the level.
     * @param height The height of the level.
     */
    Level(int width, int height) {
        level = new char[width][height];
        for (int y = 0; y < height; y++) {
            Arrays.fill(level[y], ' ');
        }
        this.width = width;
        this.height = height;
    }

    /**
     * Sets the sprite at the specified location, in the char array.
     * @param x The x coordinate of where to set the sprite.
     * @param y The y coordinate of where to set the sprite.
     * @param sprite The sprite to set at the specified coordinates.
     */
    void setSprite(int x, int y, String sprite) {
        // TODO add behaviour for cut of areas of the level (for example: fill with solid sprites)
        // TODO should probably return what sprite was previously at this coordinate
        if (!stringMapping.containsKey(sprite)) {
            stringMapping.put(sprite, mappingChar);
            charMapping.put(mappingChar, new ArrayList<>(List.of(sprite)));
            mappingChar++;
        }
        level[x][y] = stringMapping.get(sprite);
    }

    /**
     * Sets the specified sprite at any location that still has a ' ' char in the level matrix.
     * @param sprite The sprite to set (A floor sprite)
     */
    void setSpriteInEmptySpaces(String sprite) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (level[x][y] == ' ') setSprite(x, y, sprite);
            }
        }
    }

    /**
     * Transforms the level matrix into a single string.
     * @return The level matrix as a single String.
     */
    String getLevel() {
        StringBuilder result = new StringBuilder();
        for (int y = 0; y < height; y++) {
            result.append(String.valueOf(level[y])).append("\n");
        }
        return result.substring(0, result.length() - 1);
    }
}
