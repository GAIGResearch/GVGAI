package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

// slots
public class MabParameters {

  public Map<EMetaParameters, Boolean> values = new HashMap<>();

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof MabParameters)) return false;

    final MabParameters that = (MabParameters) o;

    return Objects.equals(values, that.values);
  }

  @Override
  public int hashCode() {
    return values != null ? values.hashCode() : 0;
  }

  public void addParameter(final EMetaParameters parameter, final boolean value) {
    values.put(parameter, value);
  }

  public void turnOn(final EMetaParameters parameter) {
    values.put(parameter, true);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", MabParameters.class.getSimpleName() + "[", "]")
        .add("values=" + values)
        .toString();
  }
}
