package video.basics;

public class InteractionFrame {
	
	public Interaction interaction;
	
	public String [] frames;
	
	public InteractionFrame()
	{
		
	}
	
	public InteractionFrame(Interaction interaction, String [] frames)
	{
		this.interaction = interaction;
		this.frames = frames;
	}
	
	public InteractionFrame(String interaction, String sprite1, String sprite2, String [] frames)
	{
		this.interaction = new Interaction(interaction, sprite1, sprite2);
		this.frames = frames;
	}
	
	
}
