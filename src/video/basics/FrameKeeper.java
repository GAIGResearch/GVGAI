package video.basics;

public class FrameKeeper {

	public String framesBegin [];
	public String framesEnd [];
	public int gameResult;
	
	public FrameKeeper(String framesBegin [],
	 String framesEnd [], int gameResult)
	{
		this.framesBegin = framesBegin;
		this.framesEnd = framesEnd;
		this.gameResult = gameResult;
	}
	
	public void print()
	{
		System.out.println();
		System.out.println("result: " + gameResult);
		
		System.out.println("first frames: ");
		if(framesBegin != null)
		{
			for (int i = 0; i < framesBegin.length; i++) 
			{
				System.out.println(framesBegin[i]);
			}
		}
		
		System.out.println("------------");
		
		System.out.println("last frames:");
		if(framesEnd != null)
		{
			for (int i = 0; i < framesEnd.length; i++) 
			{
				System.out.println(framesEnd[i]);
			}
		}
	}
}