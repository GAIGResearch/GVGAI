package video.gui.elements;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import video.gui.main.VideoPlayer;

/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 12/02/2018
 * @author Tiago Machado
 */

public class ShowVideoPlayerButton extends JButton implements ActionListener
{
	public String [] frames;
	public int delayTime;
	
	public ShowVideoPlayerButton(String [] frames, int delayTime) {
		addActionListener(this);
		this.frames = frames;
		this.delayTime = delayTime;
		setText("Video");
	}
	
	public ShowVideoPlayerButton() {
		addActionListener(this);
		setText("Video");
	}
	
	public void updateStringFrames(String [] frames)
	{
		this.frames = frames;
	}
	
	public void updateDelayTime(int delayTime)
	{
		this.delayTime = delayTime;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		new VideoPlayer(frames, delayTime, "");
	}

}