package tracks.singlePlayer.florabranchi.agents.meta;

import org.junit.Test;

public class MultiArmedNaiveSamplerTest {

  @Test
  public void testMabSampler() {

    MultiArmedNaiveSampler sampler = new MultiArmedNaiveSampler();
    sampler.loadMabs();
    System.out.println(sampler.localMabs);


  }

}