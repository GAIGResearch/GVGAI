package username;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class containing a level mapping, which maps characters to lists of sprites.
 */
class LevelMapping {

    /**
     * Maps characters to lists of sprites. Reverse of {@link #spritesMapping}.
     */
    @SuppressWarnings("PMD.LooseCoupling")
    @Getter private final HashMap<Character, ArrayList<String>> charMapping = new HashMap<>();

    /**
     * Maps lists of sprites to characters. Reverse of {@link #charMapping}.
     */
    private final ConcurrentHashMap<List<String>, Character> spritesMapping = new ConcurrentHashMap<>();

    /**
     * The char that gets used, when a new mapping needs to be created.
     */
    private char mappingChar = 'A';

    /**
     * Gets from the charMapping.
     * @param character A character.
     * @return The list of sprites to which the specified character is mapped.
     */
    List<String> get(char character) {
        return charMapping.get(character);
    }

    /**
     * Maps the given sprites to a char, if it wasn't in a mapping already.
     * @param sprites Strings representing sprites.
     * @return The char the sprites got mapped to.
     */
    char get(final List<String> sprites) {
        // If this mapping already exists, return the char
        if (spritesMapping.containsKey(sprites)) return spritesMapping.get(sprites);

        // Copy the list of sprites
        ArrayList<String> newSprites = new ArrayList<>(sprites);

        // Put the new mapping in both maps
        charMapping.put(mappingChar, newSprites);
        spritesMapping.put(newSprites, mappingChar);

        // Return and update the mapping character
        char character = mappingChar;
        mappingChar++;
        return character;
    }

    /**
     * Maps the given sprites to a char, if it wasn't in a mapping already.
     * @param sprites Strings representing sprites.
     * @return The char the sprites got mapped to.
     */
    char get(final String... sprites) {
        return get(Arrays.asList(sprites));
    }

    /**
     * Given a list of sprites, adds sprites to that list and creates a mapping for that new list,
     * if there didn't exist one already.
     * @param sprites A list of Strings representing sprites.
     * @param extraSprites Strings representing sprites to be added to the mapping.
     * @return The char the list of sprites got mapped to.
     */
    char getWith(List<String> sprites, List<String> extraSprites) {
        // Get the new set of sprites
        ArrayList<String> newSprites = new ArrayList<>(sprites);
        newSprites.addAll(extraSprites);

        // Create a mapping for the new set of sprites
        return get(newSprites);
    }

    /**
     * Given a list of sprites, adds sprites to that list and creates a mapping for that new list,
     * if there didn't exist one already.
     * @param sprites A list of Strings representing sprites.
     * @param extraSprites Strings representing sprites to be added to the mapping.
     * @return The char the list of sprites got mapped to.
     */
    char getWith(List<String> sprites, String... extraSprites) {
        return getWith(sprites, Arrays.asList(extraSprites));
    }

    /**
     * Given a character which is mapped to a list of sprites, adds a sprite to that list and creates a mapping for that
     * new list, if there didn't exists such a mapping already. If the character is not mapped to a list of sprites,
     * this method will create a new list with only the given sprite
     * @param character A character which is mapped to a list of sprites.
     * @param sprites Strings representing sprites to be added to the mapping.
     * @return The char the list of sprites got mapped to.
     */
    char getWith(char character, List<String> sprites) {
        return getWith(charMapping.getOrDefault(character, new ArrayList<>()), sprites);
    }

    /**
     * Given a character which is mapped to a list of sprites, adds a sprite to that list and creates a mapping for that
     * new list, if there didn't exists such a mapping already. If the character is not mapped to a list of sprites,
     * this method will create a new list with only the given sprite
     * @param character A character which is mapped to a list of sprites.
     * @param sprites Strings representing sprites to be added to the mapping.
     * @return The char the list of sprites got mapped to.
     */
    char getWith(char character, String... sprites) {
        return getWith(character, Arrays.asList(sprites));
    }

    /**
     * Given a list of sprites, removes sprites from that lists and creates a mapping for that new list,
     * if there didn't exist one already.
     * @param sprites A list of Strings representing sprites.
     * @param removeSprites Strings representing sprites to be removed from the mapping.
     * @return The char the list of sprites got mapped to.
     */
    char getWithout(List<String> sprites, List<String> removeSprites) {
        // Get the new set of sprites
        ArrayList<String> newSprites = new ArrayList<>(sprites);
        newSprites.removeAll(removeSprites);

        // Create a mapping for the new set of sprites
        return get(newSprites);
    }

    /**
     * Given a list of sprites, removes sprites from that lists and creates a mapping for that new list,
     * if there didn't exist one already.
     * @param sprites A list of Strings representing sprites.
     * @param removeSprites Strings representing sprites to be removed from the mapping.
     * @return The char the list of sprites got mapped to.
     */
    char getWithout(List<String> sprites, String... removeSprites) {
        return getWithout(sprites, Arrays.asList(removeSprites));
    }

    /**
     * Given a character which is mapped to a list of sprites, removes sprites from that list and creates a mapping for
     * that new list, if there didn't exists such a mapping already. If the character is not mapped to a list of
     * sprites, this method will create a new list with only the given sprite
     * @param character A character which is mapped to a list of sprites.
     * @param removeSprites Strings representing sprites to be removed from the mapping.
     * @return The char the list of sprites got mapped to.
     */
    char getWithout(char character, List<String> removeSprites) {
        return getWithout(charMapping.getOrDefault(character, new ArrayList<>()), removeSprites);
    }

    /**
     * Given a character which is mapped to a list of sprites, removes sprites from that list and creates a mapping for
     * that new list, if there didn't exists such a mapping already. If the character is not mapped to a list of
     * sprites, this method will create a new list with only the given sprite
     * @param character A character which is mapped to a list of sprites.
     * @param removeSprites Strings representing sprites to be removed from the mapping.
     * @return The char the list of sprites got mapped to.
     */
    char getWithout(char character, String... removeSprites) {
        return getWithout(character, Arrays.asList(removeSprites));
    }
}
