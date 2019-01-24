package agents.MaastCTS2.model;

public class Score {
	public double timesVisited;
	public double score;

	public Score() {
		this(0.0, 0.0);
	}

	public Score(double timesVisited, double initialScore) {
		this.timesVisited = timesVisited;
		this.score = initialScore;
	}
	
	public void decay(double decayFactor){
		timesVisited *= decayFactor;
		score *= decayFactor;
	}

	public double getAverageScore() {
		return score / timesVisited;
	}
}
