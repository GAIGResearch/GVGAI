package tracks.singlePlayer.florabranchi.agents.meta;

import org.junit.Test;

public class MultiArmedNaiveSamplerTest {

  @Test
  public void testMabSampler() {

    MultiArmedNaiveSampler sampler = new MultiArmedNaiveSampler();


    final MabParameters greedyMabParameters = sampler.addRandomSample();
    final MabParameters greedyMabParameters2 = sampler.exploitMabs();

    sampler.updateMabData(greedyMabParameters2, 10);

    final MabParameters mabParameters = sampler.mabExploration();
    System.out.println(mabParameters);


  }

}