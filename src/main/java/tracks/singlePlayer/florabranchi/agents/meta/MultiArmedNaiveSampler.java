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

  // pi l
  int EXPLORATION_POLICY_EPSILON = 50;

  // pi g
  int EXPLOITATION_EPSILON = 0;

  // local mabs
  public Map<EMetaParameters, LocalMabData> localMabs = new HashMap<>();

  // Known global mabs hash map - values hash is key
  public Map<MabParameters, GlobalMabData> globalMab = new HashMap<>();

  public Set<Integer> sampledGlobalMabs = new HashSet<>();

  // episilon l e g


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
    //addRandomSample();
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


  public MabParameters exploitMabs() {

    // Either try again with existing globals or use best global
    int rand = random.nextInt(100);
    if (rand <= EXPLOITATION_EPSILON) {
      return getRandomGlobalMab();
    }

    double maxPerceivedReward = 0;
    MabParameters maxMab = null;
    List<MabParameters> drawResults = new ArrayList<>();
    for (Map.Entry<MabParameters, GlobalMabData> mabParametersGlobalMabDataEntry : globalMab.entrySet()) {

      final double entryReward = mabParametersGlobalMabDataEntry.getValue().getAverageReward();

      if (maxMab == null || entryReward > maxPerceivedReward) {
        maxMab = mabParametersGlobalMabDataEntry.getKey();
        drawResults.clear();
        drawResults.add(maxMab);
        maxPerceivedReward = entryReward;
      } else if (entryReward == maxPerceivedReward) {
        maxMab = mabParametersGlobalMabDataEntry.getKey();
        drawResults.add(maxMab);
      }
    }

    if (drawResults.size() > 1) {
      System.out.println("Using Random maximized mab - draw between mabs");
      return drawResults.get(random.nextInt(drawResults.size()));


    } else if (drawResults.isEmpty()) {
      System.out.println("No Exploit MABs available - adding random sample");
      return exploreMabs();
    } else {


      // if draw return any
      maxMab = drawResults.get(0);
      if (maxPerceivedReward < 1) {
        System.out.println("Using existing GlobalMab");
        return getRandomGlobalMab();
      }
      return maxMab;
    }
  }

  public MabParameters exploreMabs() {

    final MabParameters mabParameters = emptyMab();
    for (Map.Entry<EMetaParameters, LocalMabData> entry : localMabs.entrySet()) {
      int rand = random.nextInt(100);
      if (rand <= EXPLORATION_POLICY_EPSILON) {
        // Random value
        mabParameters.addParameter(entry.getKey(), random.nextBoolean());
      } else {
        // Use LocalMabs evaluation to select true or false
        final LocalMabData.LocalMabInfo onInfo = entry.getValue().localMabData.get(true);
        final LocalMabData.LocalMabInfo offInfo = entry.getValue().localMabData.get(false);
        final double onAvgRwd = onInfo.getAverageReward();
        final double offAvgRwd = offInfo.getAverageReward();
        final boolean bestValue = onAvgRwd >= offAvgRwd;

        mabParameters.addParameter(entry.getKey(), bestValue);
      }
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

     if (globalMab.containsKey(mabParameters)) {
      globalMab.get(mabParameters).timesSelected++;
      globalMab.get(mabParameters).totalRewards += reward;
    } else {
      System.out.println("Could not find mab in global mabs. hash code: " + mabParameters.hashCode());
      System.out.println("Existing mabs hashes:" + sampledGlobalMabs);
    }


    final Map<EMetaParameters, Boolean> values = mabParameters.values;
    for (Map.Entry<EMetaParameters, Boolean> eMetaParametersBooleanEntry : values.entrySet()) {
      final LocalMabData localMabData = localMabs.get(eMetaParametersBooleanEntry.getKey());
      final LocalMabData.LocalMabInfo localMabInfo = localMabData.localMabData.get(eMetaParametersBooleanEntry.getValue());
      localMabInfo.marginalizedAvgScoreForParameter += reward;
      localMabInfo.timesParameterSelected++;
    }
    updateBanditArms();
  }


}
