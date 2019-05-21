package username;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@SuppressWarnings("PMD.LooseCoupling")
class LevelMappingTest {

	private LevelMapping levelMapping;

	@BeforeEach void beforeEach() {
		levelMapping = new LevelMapping();
	}

	@Test void empty() {
		assertThat(levelMapping.getCharMapping()).isEmpty();
	}

	@Test void putOne() {
		ArrayList<String> spritesInput = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected = new ArrayList<>(List.of("s1", "s2", "s3"));

		putAndAssert(spritesInput, 'A');

		assertThat(levelMapping.getCharMapping()).containsOnly(entry('A', spritesExpected));
	}

	@Test void putSameTwice() {
		ArrayList<String> spritesInput1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesInput2 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected = new ArrayList<>(List.of("s1", "s2", "s3"));

		putAndAssert(spritesInput1, 'A');
		putAndAssert(spritesInput2, 'A');

		assertThat(levelMapping.getCharMapping()).as("If we try to put the same list of sprites in the map twice,"
				+ "it should only be added once.").containsOnly(entry('A', spritesExpected));
	}

	@Test void putThree() {
		ArrayList<String> spritesInput1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesInput2 = new ArrayList<>(List.of("s1", "s2", "s3", "s4"));
		ArrayList<String> spritesInput3 = new ArrayList<>(List.of("s1", "s2", "s3", "s5"));

		putAndAssert(spritesInput1, 'A');
		putAndAssert(spritesInput2, 'B');
		putAndAssert(spritesInput3, 'C');

		assertThat(levelMapping.getCharMapping()).hasSize(3);
	}

	@Test void expandMappingList() {
		ArrayList<String> spritesInput1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesInput2 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected2 = new ArrayList<>(List.of("s1", "s2", "s3", "s4"));

		putAndAssert(spritesInput1, 'A');
		levelMapping.expandMapping(spritesInput2, "s4");

		assertThat(levelMapping.getCharMapping()).as("If we expand the mapping of a list of sprites,"
						+ "a new list of sprites (with the added sprite) should be added.").containsOnly(
				entry('A', spritesExpected1), entry('B', spritesExpected2));
	}

	@Test void expandMappingChar() {
		ArrayList<String> spritesInput1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected2 = new ArrayList<>(List.of("s1", "s2", "s3", "s4"));

		putAndAssert(spritesInput1, 'A');
		assertThat(levelMapping.expandMapping('A', "s4")).isEqualTo('B');

		assertThat(levelMapping.getCharMapping()).containsOnly(
				entry('A', spritesExpected1), entry('B', spritesExpected2));
	}

	@Test void expandMappingCharNotFound() {
		ArrayList<String> spritesInput1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected1 = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected2 = new ArrayList<>(List.of("s4"));

		putAndAssert(spritesInput1, 'A');
		assertThat(levelMapping.expandMapping('Z', "s4")).isEqualTo('B');

		assertThat(levelMapping.getCharMapping()).as("If the given char does not map to a list,"
				+ "a new list should be created.").containsOnly(entry('A', spritesExpected1),
				entry('B', spritesExpected2));
	}

	@Test void modifyInputList() {
		ArrayList<String> spritesInput = new ArrayList<>(List.of("s1", "s2", "s3"));
		ArrayList<String> spritesExpected = new ArrayList<>(List.of("s1", "s2", "s3"));

		putAndAssert(spritesInput, 'A');
		spritesInput.add("s4");

		assertThat(levelMapping.get('A')).isEqualTo(spritesExpected);
		assertThat(levelMapping.get(spritesExpected)).isEqualTo('A');
	}

	private void putAndAssert(ArrayList<String> sprites, char expectedChar) {
		assertThat(levelMapping.get(sprites)).isEqualTo(expectedChar);

		ArrayList<String> spritesExpected = new ArrayList<>(sprites);
		assertThat(levelMapping.get(expectedChar)).isEqualTo(spritesExpected);
		assertThat(levelMapping.get(spritesExpected)).isEqualTo(expectedChar);
	}
}
