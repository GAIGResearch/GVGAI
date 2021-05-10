package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import tracks.singlePlayer.florabranchi.database.BanditArmsData;
import tracks.singlePlayer.florabranchi.persistence.PropertyLoader;

public class MultiArmedNaiveSampler {

  public BanditArmsData banditArmsData;

  Random random = new Random();

  int EXPLORATION_EPSILON = 10;

  // local mabs
  public Map<EMetaParameters, LocalMabData> localMabs = new HashMap<>();

  // Known global mabs hash map - values hash is key
  public Map<MabParameters, GlobalMabData> globalMab = new HashMap<>();

  public void updateBanditArms() {
    banditArmsData.updateArms(localMabs, globalMab);
  }

  public MultiArmedNaiveSampler(final BanditArmsData banditArmsData) {
    this.banditArmsData = banditArmsData;
    this.localMabs = banditArmsData.getLocalMabs();
    this.globalMab = banditArmsData.getGlobalMabs();
  }

  public MultiArmedNaiveSampler() {
    banditArmsData = new BanditArmsData();
    addLocalMabs();
    // add initial random mab
    addRandomSample();
    updateBanditArms();
  }

  public MabParameters emptyMab() {
    MabParameters mabParameters = new MabParameters();
    for (EMetaParameters value : getAllParameters()) {
      mabParameters.addParameter(value, false);
    }
    return mabParameters;
  }

  EMetaParameters[] getAllParameters() {
    return EMetaParameters.values();
  }

  public MabParameters getRandomGlobalMab() {
    final List<MabParameters> mabParameters = new ArrayList<>(globalMab.keySet());
    return mabParameters.get(random.nextInt(mabParameters.size()));
  }

  public MabParameters addRandomSample() {
    final MabParameters globalMab = emptyMab();
    for (EMetaParameters value : getAllParameters()) {
      globalMab.addParameter(value, random.nextBoolean());
    }
    addToGlobalMab(globalMab);
    return globalMab;
  }

  private void addToGlobalMab(final MabParameters globalMab) {
    GlobalMabData globalMabData = new GlobalMabData();
    this.globalMab.put(globalMab, globalMabData);
  }

  public MabParameters mabExploration() {

    // e-greedy for exploration
    int rand = random.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      System.out.println("selecting exploration play - add random mab --------------------------------");
      return addRandomSample();
    }

    System.out.println("selecting exploitation play for exploration - add best mab --------------------------------");

    // mab exploitation
    // select best expected rewards
    List<EMetaParameters> explorationBestParameters = new ArrayList<>();
    for (Map.Entry<EMetaParameters, LocalMabData> entry : localMabs.entrySet()) {
      if (entry.getValue().getAverageReward() > 1) {
        explorationBestParameters.add(entry.getKey());
      }
    }

    final MabParameters mabParameters = emptyMab();
    explorationBestParameters.forEach(metaParameter -> mabParameters.addParameter(metaParameter, true));

    // Try selecting a different version if the most optimized one exists
    int tries = 2;
    while (globalMab.containsKey(mabParameters) && tries > 0) {
      mabParameters.randomMutation(random);
      tries--;
    }
    return mabParameters;
  }


  void addLocalMabs() {
    final EMetaParameters[] values = getAllParameters();
    for (EMetaParameters value : values) {
      localMabs.put(value, new LocalMabData());
    }
  }

  public void updateMabData(final MabParameters mabParameters,
                            final double reward) {

    globalMab.get(mabParameters).timesSelected++;
    globalMab.get(mabParameters).totalRewards += reward;

    final Map<EMetaParameters, Boolean> values = mabParameters.values;
    for (Map.Entry<EMetaParameters, Boolean> eMetaParametersBooleanEntry : values.entrySet()) {
      if (eMetaParametersBooleanEntry.getValue()) {
        final LocalMabData localMabData = localMabs.get(eMetaParametersBooleanEntry.getKey());
        localMabData.marginalizedAvgScoreForParameter += reward;
        localMabData.timesParameterSelected++;
      }
    }
    updateBanditArms();
  }

  MabParameters exploitMabs() {

    double maxPerceivedReward = 0;
    MabParameters maxMab = null;
    for (Map.Entry<MabParameters, GlobalMabData> mabParametersGlobalMabDataEntry : globalMab.entrySet()) {

      if (maxMab == null || mabParametersGlobalMabDataEntry.getValue().getAverageReward() > maxPerceivedReward) {
        maxPerceivedReward = mabParametersGlobalMabDataEntry.getValue().getAverageReward();
        maxMab = mabParametersGlobalMabDataEntry.getKey();
      }
    }

    // if draw return any
    if (maxMab == null || maxPerceivedReward < 1) {
      System.out.println("Using Random MAB since exploitaition would yield bad resuls");
      return addRandomSample();
    }

    return maxMab;
  }


}
