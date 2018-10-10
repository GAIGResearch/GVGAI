package video.gui.main;

import javax.swing.JFrame;
import video.gui.elements.FrameLabel;
import video.gui.elements.PlayVideoButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 06/02/2018
 * @author Tiago Machado
 */

public class VideoPlayer extends JFrame
{
	
	public String [] frames;
	public FrameLabel frameToShow;
	public int delayTime;
	public PlayVideoButton playVideoButton;
	public JLabel subtitleLabel;
	
	public VideoPlayer(String [] frames, int delayTime, String subtitleString)
	{
		setBounds(0, 0, 455, 356);
		this.frames = frames;
		this.delayTime = delayTime;
		getContentPane().setLayout(null);
		frameToShow = new FrameLabel(6, 6, 438, 266);
		if(!checkNulity())
			frameToShow.updateFrame(frames[0]);
		getContentPane().add(frameToShow);
		
		playVideoButton = new PlayVideoButton(this.frames, frameToShow, this.delayTime);
		playVideoButton.setBounds(165, 303, 117, 29);
		getContentPane().add(playVideoButton);
		
		subtitleLabel = new JLabel(subtitleString);
		subtitleLabel.setBounds(16, 284, 428, 16);
		subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		getContentPane().add(subtitleLabel);
		
		setVisible(true);	
	}
	
	public boolean checkNulity()
	{
		if(frames != null)
		{
			for (int i = 0; i < frames.length; i++) {
				if(frames[i] == null)
					return true;
			}
		}	
		return false;
	}
}