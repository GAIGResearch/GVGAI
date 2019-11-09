package video.gui.elements;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import video.handlers.FrameInteractionAssociation;

public class RetrieveButton extends JButton implements ActionListener{

	public FrameInteractionAssociation frameInteractionAssociation;
	public FrameLabel [] frameLabel;
	public ComboBox interactionComboBox;
	public String[] frames;
	private ShowVideoPlayerButton showVideoPlayerButton;
	
	public RetrieveButton(FrameInteractionAssociation frameInteractionAssociation, 
			ComboBox interactionComboBox, 
				FrameLabel [] frameLabel,
					ShowVideoPlayerButton showVideoPlayerButton) throws FileNotFoundException, IOException, ParseException {
		super("Retrieve Frames");
		setBounds(544, 6, 130, 29);
		addActionListener(this);
		this.frameLabel = frameLabel;
		this.frameInteractionAssociation = frameInteractionAssociation;
		this.interactionComboBox = interactionComboBox;
		this.showVideoPlayerButton = showVideoPlayerButton;
		
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		String interaction = interactionComboBox.getSelectedItem().toString();
		int index = interactionComboBox.getSelectedIndex();
		String tick = interactionComboBox.interaction[index].gameTick;
		
		System.out.println("index " + index);
		System.out.println("tick " + tick);
		
		JSONObject iteractionObject = 
				this.frameInteractionAssociation.retrieveInteraction(interaction, tick);
		
		frames = this.frameInteractionAssociation.retrieveInteractionFrames(iteractionObject);
		this.frameLabel[0].updateFrame(frames[0]);
		this.frameLabel[1].updateFrame(frames[1]);
		this.frameLabel[2].updateFrame(frames[2]);
		
		showVideoPlayerButton.updateStringFrames(getStringFrames());
		showVideoPlayerButton.setVisible(true);
	}
	
	public String [] getStringFrames()
	{
		return new String[]{this.frameLabel[0].getThisStringFrame(),
							this.frameLabel[1].getThisStringFrame(),
							this.frameLabel[2].getThisStringFrame()};
	}

}