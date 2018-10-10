package video.gui.elements;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

import org.json.simple.JSONObject;

/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 12/02/2018
 * @author Tiago Machado
 */

public class PlayVideoButton extends JButton implements ActionListener, Runnable
{
	public String [] frames;
	public FrameLabel frameLabel;
	public long timeDelay;
	public int frameIndex;
	private Thread videoThread;
	
	public PlayVideoButton(String [] frames, FrameLabel frameLabel, long timeDelay)
	{
		super();
		addActionListener(this);
		this.setText("Play");
		this.frames = frames;
		this.frameLabel = frameLabel;
		this.timeDelay = timeDelay;
		this.frameIndex = 0;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) 
	{
		if(this.getText().equals("Stop"))
		{
			videoThread.interrupt();
			this.setText("Play");
			return;
		}
		
		if(this.getText().equals("Play"))
		{
			videoThread = new Thread(this);
			videoThread.start();
			this.setText("Stop");
			return;
		}
		
	}
	
	public void play() throws InterruptedException {
		// TODO Auto-generated method stub
		new Thread().sleep(timeDelay);
		for (;;) {
			frameIndex++;
			if(frameIndex > 4)
				frameIndex = 0;
			System.out.println(frameIndex);
			System.out.println(this.frames[frameIndex]);
			frameLabel.updateFrame(this.frames[frameIndex]);
			new Thread().sleep(timeDelay);
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			play();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println("Interrupted");
		}
	}
}