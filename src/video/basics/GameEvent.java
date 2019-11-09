package video.basics;

public class GameEvent implements Comparable<GameEvent> {
	public String gameTick;

	@Override
	public int compareTo(GameEvent o) {
		return this.toString().compareTo(o.toString());
	}
	
	public boolean equals(GameEvent e) {
		return this.toString().equals(e.toString());
	}
	

}
