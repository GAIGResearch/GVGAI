package username;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class containing a matrix of sets, containing strings which represent sprites, representing a level.
 */
public class Level {

    /**
     * The level we are generating, represented as a matrix of sets, containing strings which represent sprites.
     * The outer list contains the rows, i.e. y coordinate.
     * The middle lists contain sprite lists, i.e. x coordinate.
     * The inner lists are sprite lists.
     */
    private List<List<ArrayList<String>>> level;

    /**
     * The width of the level.
     */
    @Getter private int width;

    /**
     * The height of the level.
     */
    @Getter private int height;

    /**
     * The levelMapping for this level. Only gets generated, once {@link #getLevel()} gets called.
     */
    @Getter private HashMap<Character, ArrayList<String>> levelMapping;

    /**
     * Maps every string to a list of strings, according to the default level mapping.
     */
    private HashMap<String, ArrayList<String>> specialMappings = new HashMap<>();

    /**
     * Constructs a new level, of the specified size.
     * @param width The width of the level.
     * @param height The height of the level.
     * @param levelMapping The default level mapping for this level.
     */
    Level(int width, int height, HashMap<Character, ArrayList<String>> levelMapping) {
        this.width = width;
        this.height = height;

        // Initialize the level matrix
        level = new ArrayList<>(height);
        for (int y = 0; y < height; y++) {
            List<ArrayList<String>> row = new ArrayList<>(width);
            level.add(row);
            for (int x = 0; x < width; x++) {
                row.add(new ArrayList<>(1));
            }
        }

        // Initialize specialMappings
        Set<String> ambiguousSprites = new HashSet<>();
        levelMapping.values().forEach(sprites -> sprites.forEach(sprite -> {
            if (!ambiguousSprites.contains(sprite)) {
                if (specialMappings.containsKey(sprite)) {
                    ambiguousSprites.add(sprite);
                    specialMappings.remove(sprite);
                } else {
                    specialMappings.put(sprite, sprites);
                }
            }
        }));
    }

    /**
     * Adds the sprite at the specified location, in the level matrix.
     * @param x The x coordinate of where to set the sprite.
     * @param y The y coordinate of where to set the sprite.
     * @param sprite The sprite to set at the specified coordinates.
     */
    void addSprite(int x, int y, String sprite) {
        List<String> newSprites = specialMappings.getOrDefault(sprite, new ArrayList<>(List.of(sprite)));
        ArrayList<String> sprites = level.get(y).get(x);
        sprites.addAll(newSprites);
    }

    /**
     * Sets the sprite at the specified location, in the level matrix.
     * @param x The x coordinate of where to set the sprite.
     * @param y The y coordinate of where to set the sprite.
     * @param sprite The sprite to set at the specified coordinates.
     */
    void setSprite(int x, int y, String sprite) {
        // TODO add behaviour for cut of areas of the level (for example: fill with solid sprites)
        // TODO should probably return which sprites were previously at this coordinate
        level.get(y).set(x, specialMappings.getOrDefault(sprite, new ArrayList<>(List.of(sprite))));
    }

    /**
     * Sets the specified sprite at any location that has 0 sprites set, in the level matrix.
     * @param sprite The sprite to set (A floor sprite)
     */
    void setSpriteInEmptySpaces(String sprite) {
        for (int y = 0; y < level.size(); y++) {
            List<ArrayList<String>> row = level.get(y);
            for (int x = 0; x < row.size(); x++) {
                if (row.get(x).isEmpty()) setSprite(x, y, sprite);
            }
        }
    }

    /**
     * Transforms the level matrix into a single string.
     * @return The level matrix as a single String.
     */
    String getLevel() {
        levelMapping = new HashMap<>();
        HashMap<List<String>, Character> listMapping = new HashMap<>();
        char mappingChar = 'a';

        StringBuilder result = new StringBuilder();
        for (List<ArrayList<String>> row : level) {
            for (ArrayList<String> sprites : row) {
                // Add a char to the levelMapping for this list of sprites, if there isn't any
                if (listMapping.putIfAbsent(sprites, mappingChar) == null) {
                    levelMapping.put(mappingChar, sprites);
                    mappingChar++;
                }

                result.append(listMapping.get(sprites));
            }

            result.append("\n");
        }

        return result.substring(0, result.length() - 1);
    }
}
