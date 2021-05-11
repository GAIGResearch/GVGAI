package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import tracks.singlePlayer.florabranchi.database.BanditArmsData;

public class MultiArmedNaiveSampler {

  public BanditArmsData banditArmsData;

  Random random = new Random();

  int EXPLORATION_EPSILON = 10;

  // local mabs
  public Map<EMetaParameters, LocalMabData> localMabs = new HashMap<>();

  // Known global mabs hash map - values hash is key
  public Map<MabParameters, GlobalMabData> globalMab = new HashMap<>();

  public Set<Integer> sampledGlobalMabs = new HashSet<>();


  public void updateBanditArms() {
    banditArmsData.updateArms(localMabs, globalMab);
  }

  public MultiArmedNaiveSampler(final BanditArmsData banditArmsData) {
    this.banditArmsData = banditArmsData;
    this.localMabs = banditArmsData.getLocalMabs();
    this.globalMab = banditArmsData.getGlobalMabs();
    this.sampledGlobalMabs = globalMab.keySet().stream().map(MabParameters::hashCode).collect(Collectors.toSet());
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
    final MabParameters newMab = emptyMab();
    for (EMetaParameters value : getAllParameters()) {
      newMab.addParameter(value, random.nextBoolean());
    }

    int tries = 10;
    while (sampledGlobalMabs.contains(newMab.hashCode()) && tries > 0) {
      newMab.randomMutation(random);
      tries--;
    }

    if (sampledGlobalMabs.contains(newMab.hashCode())) {
      // could not get original mab. proceed with random
      return getRandomGlobalMab();
    }

    addToGlobalMab(newMab);
    return newMab;
  }

  private void addToGlobalMab(final MabParameters globalMab) {
    GlobalMabData globalMabData = new GlobalMabData();
    this.globalMab.put(globalMab, globalMabData);
    this.sampledGlobalMabs.add(globalMab.hashCode());
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

  public MabParameters exploreMabs() {

    // e-greedy for exploration
    int rand = random.nextInt(100);
    if (rand <= EXPLORATION_EPSILON) {
      System.out.println("selecting exploration play - return random existing mab --------------------------------");
      return getRandomGlobalMab();
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
    int tries = 10;
    while (sampledGlobalMabs.contains(mabParameters.hashCode()) && tries > 0) {
      mabParameters.randomMutation(random);
      tries--;
    }
    if (!sampledGlobalMabs.contains(mabParameters.hashCode())) {
      addToGlobalMab(mabParameters);
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


}
