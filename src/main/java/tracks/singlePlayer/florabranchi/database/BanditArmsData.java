package tracks.singlePlayer.florabranchi.database;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Embeddable;

import tracks.singlePlayer.florabranchi.agents.meta.EMetaParameters;
import tracks.singlePlayer.florabranchi.agents.meta.GlobalMabData;
import tracks.singlePlayer.florabranchi.agents.meta.LocalMabData;
import tracks.singlePlayer.florabranchi.agents.meta.MabParameters;

public class BanditArmsData implements Serializable {

  public List<ArmData> armDataList = new ArrayList<>();

  public List<LocalArmData> localArmData = new ArrayList<>();

  public Map<EMetaParameters, LocalMabData> getLocalMabs() {

    Map<EMetaParameters, LocalMabData> map = new HashMap<>();
    localArmData.forEach(entry -> map.put(entry.metaParameters,
        new LocalMabData(entry.marginalizedAvgScoreForParameter, entry.timesParameterSelected)));
    return map;
  }

  public Map<MabParameters, GlobalMabData> getGlobalMabs() {
    Map<MabParameters, GlobalMabData> map = new HashMap<>();
    armDataList.forEach(entry -> map.put(entry.toMabParameter(), entry.toGlobalMab()));
    return map;
  }

  public void updateArms(Map<EMetaParameters, LocalMabData> localMabs,
                         Map<MabParameters, GlobalMabData> globalMab) {
    localMabs.forEach((key, value) -> localArmData.add(new LocalArmData(key, value)));
    globalMab.forEach((key, value) -> armDataList.add(new ArmData(key, value)));
  }

  public BanditArmsData(final List<ArmData> armDataList, final List<LocalArmData> localArmData) {
    this.armDataList = armDataList;
    this.localArmData = localArmData;
  }

  public BanditArmsData(Map<EMetaParameters, LocalMabData> localMabs,
                        Map<MabParameters, GlobalMabData> globalMab) {
    updateArms(localMabs, globalMab);

    for (Map.Entry<EMetaParameters, LocalMabData> localMabsIt : localMabs.entrySet()) {

      /*switch (localMabsIt.getKey()) {
        case TREE_REUSE:
          treeReuseArmData = new LocalArmData(localMabsIt.getValue());
          break;
        case RAW_GAME_SCORE:
          rawScoreArmData = new LocalArmData(localMabsIt.getValue());
          break;
        case MACRO_ACTIONS:
          macroActionsArmData = new LocalArmData(localMabsIt.getValue());
          break;
        case LOSS_AVOIDANCE:
          lossAvoidanceArmData = new LocalArmData(localMabsIt.getValue());
          break;
        case EARLY_INITIALIZATION:
          earlyInitArmData = new LocalArmData(localMabsIt.getValue());
          break;
        case SELECT_HIGHEST_SCORE_CHILD:
          highestScoreChildArmData = new LocalArmData(localMabsIt.getValue());
          break;
      }*/
    }

  }

  public BanditArmsData() {
  }

  @Embeddable
  static
  class ArmData implements Serializable {

    public boolean treeReuse;
    public boolean rawGameScore;
    public boolean macroActions;
    public boolean lossAvoidance;
    public boolean earlyInitialization;
    public boolean selectHighestScoreChild;
    public double totalRewards;
    public int timesSelected;

    public ArmData(final MabParameters mabParameters,
                   final GlobalMabData globalMabData) {
      treeReuse = mabParameters.getParameter(EMetaParameters.EARLY_INITIALIZATION);
      rawGameScore = mabParameters.getParameter(EMetaParameters.RAW_GAME_SCORE);
      macroActions = mabParameters.getParameter(EMetaParameters.MACRO_ACTIONS);
      lossAvoidance = mabParameters.getParameter(EMetaParameters.LOSS_AVOIDANCE);
      earlyInitialization = mabParameters.getParameter(EMetaParameters.EARLY_INITIALIZATION);
      selectHighestScoreChild = mabParameters.getParameter(EMetaParameters.SELECT_HIGHEST_SCORE_CHILD);
      totalRewards = globalMabData.totalRewards;
      timesSelected = globalMabData.timesSelected;
    }

    public MabParameters toMabParameter() {
      MabParameters mabParameters = new MabParameters();
      mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, treeReuse);
      mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, rawGameScore);
      mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, macroActions);
      mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, lossAvoidance);
      mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, earlyInitialization);
      mabParameters.addParameter(EMetaParameters.EARLY_INITIALIZATION, selectHighestScoreChild);
      return mabParameters;
    }

    public GlobalMabData toGlobalMab() {
      GlobalMabData data = new GlobalMabData();
      data.timesSelected = this.timesSelected;
      data.totalRewards = this.totalRewards;
      return data;
    }

    public ArmData() {

    }
  }

  @Embeddable
  static
  class LocalArmData implements Serializable {

    public double marginalizedAvgScoreForParameter;
    public double timesParameterSelected;
    public EMetaParameters metaParameters;
    public String metaParameterString;

    public LocalArmData(final EMetaParameters metaParameters,
                        final LocalMabData localArmData) {

      marginalizedAvgScoreForParameter = localArmData.marginalizedAvgScoreForParameter;
      timesParameterSelected = localArmData.timesParameterSelected;
      this.metaParameters = metaParameters;
      this.metaParameterString = metaParameters.toString();
    }

    public LocalArmData() {

    }
  }


}
