package tracks.singlePlayer.florabranchi.agents.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tracks.singlePlayer.florabranchi.database.BanditArmsData;

public class MultiArmedNaiveSampler {

  public BanditArmsData banditArmsData;

  Random random = new Random();

  // local mabs
  public Map<EMetaParameters, LocalMabData> localMabs = new HashMap<>();

  // Known global mabs hash map - values hash is key
  public Map<MabParameters, GlobalMabData> globalMab = new HashMap<>();

  public MultiArmedNaiveSampler(final Map<EMetaParameters, LocalMabData> localMabs,
                                final Map<MabParameters, GlobalMabData> globalMab) {
    this.localMabs = localMabs;
    this.globalMab = globalMab;
  }

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
    GlobalMabData globalMabData = new GlobalMabData();
    this.globalMab.put(globalMab, globalMabData);
    return globalMab;
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
  }

  MabParameters exploitMabs() {

    double maxPerceivedReward = 0;
    MabParameters maxMab = null;
    for (Map.Entry<MabParameters, GlobalMabData> mabParametersGlobalMabDataEntry : globalMab.entrySet()) {

      if (mabParametersGlobalMabDataEntry.getValue().getAverageReward() > maxPerceivedReward) {
        maxPerceivedReward = mabParametersGlobalMabDataEntry.getValue().getAverageReward();
        maxMab = mabParametersGlobalMabDataEntry.getKey();
      }
    }

    // if draw return any
    if (maxMab == null) {
      return getRandomGlobalMab();
    }

    return maxMab;
  }


}
