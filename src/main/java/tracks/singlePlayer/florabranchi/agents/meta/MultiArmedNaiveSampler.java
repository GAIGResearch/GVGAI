package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class MultiArmedNaiveSampler {

  Random random = new Random();

  // local mabs
  public Map<EMetaParameters, MabParameters> localMabs = new HashMap<>();

  // Known global mabs hash map - values hash is key
  public Map<Integer, MabParameters> globalMab = new HashMap<>();

  MabParameters emptyMab() {
    MabParameters mabParameters = new MabParameters();
    for (EMetaParameters value : getAllParameters()) {
      mabParameters.addParameter(value, false);
    }
    return mabParameters;
  }

  EMetaParameters[] getAllParameters() {
    return EMetaParameters.values();
  }

  public void loadMabs() {

    // try to retrieve from persistence

    // new
    addLocalMabs();
  }

  MabParameters addRandomSample() {
    final MabParameters globalMab = emptyMab();
    for (EMetaParameters value : getAllParameters()) {
      globalMab.addParameter(value, random.nextBoolean());
    }
    this.globalMab.put(globalMab.hashCode(), globalMab);
    return globalMab;
  }


  void addLocalMabs() {
    final EMetaParameters[] values = getAllParameters();
    for (EMetaParameters value : values) {
      final MabParameters globalMab = emptyMab();
      globalMab.turnOn(value);
      localMabs.put(value, globalMab);
    }
  }


}
