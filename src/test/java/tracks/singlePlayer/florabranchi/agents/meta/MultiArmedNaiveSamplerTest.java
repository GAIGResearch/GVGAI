package tracks.singlePlayer.florabranchi.agents.meta;

import org.junit.Test;

public class MultiArmedNaiveSamplerTest {

  @Test
  public void testMabSampler() {

    MultiArmedNaiveSampler sampler = new MultiArmedNaiveSampler();


    final MabParameters greedyMabParameters = sampler.addRandomSample();
    final MabParameters greedyMabParameters2 = sampler.exploitMabs();

    sampler.updateMabData(greedyMabParameters2, 10);

    MabParameters mabParameters = sampler.exploreMabs();
    mabParameters = sampler.exploreMabs();
    sampler.updateMabData(mabParameters, -10);
    mabParameters = sampler.exploreMabs();
    sampler.updateMabData(mabParameters, -10);
    mabParameters = sampler.exploitMabs();
    sampler.updateMabData(mabParameters, 100);

    mabParameters = sampler.exploitMabs();
    System.out.println(mabParameters);


  }

}