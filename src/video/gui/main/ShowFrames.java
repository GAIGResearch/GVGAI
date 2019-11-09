package video.gui.main;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.swing.JTextField;
import org.json.simple.parser.ParseException;
import video.constants.InteractionsList;
import video.gui.elements.ComboBox;
import video.gui.elements.FrameLabel;
import video.gui.elements.RetrieveButton;
import video.gui.elements.ShowVideoPlayerButton;
import video.handlers.FrameInteractionAssociation;
import video.utils.Utils;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Code written by Tiago Machado (tiago.machado@nyu.edu)
 * Date: 12/02/2018
 * @author Tiago Machado
 */

public class ShowFrames extends JFrame{
	
	public FrameInteractionAssociation frameInteractionAssociation;
	public FrameLabel [] frameLabel;
	public RetrieveButton retrieveButton;
	public String interactionFileName;
	public ComboBox comboBoxInteractions;
	private JLabel lblPreviousFrame;
	private JLabel lblInteractionFrame;
	private JLabel lblAfterInteraction;
	private ShowVideoPlayerButton showVideoPlayerButton;
	
	public ShowFrames() throws FileNotFoundException, IOException, ParseException 
	{
		getContentPane().setLayout(null);
		setBounds(10,10,1080,320);
		interactionFileName = "interaction/interaction.json";
		frameInteractionAssociation = new FrameInteractionAssociation(interactionFileName);
		
		startComboBoxes();
		startLabels();
		startButtons();
		
		setVisible(true);
	}
	
	public void startButtons() throws FileNotFoundException, IOException, ParseException
	{
		startShowVideoPlayerButton();
		retrieveButton = 
				new RetrieveButton(frameInteractionAssociation, 
						comboBoxInteractions, 
						frameLabel, 
						showVideoPlayerButton);
		getContentPane().add(retrieveButton);
	}

	/**
	 * 
	 */
	public void startShowVideoPlayerButton() {
		{
			showVideoPlayerButton = new ShowVideoPlayerButton(null, 100);
			showVideoPlayerButton.setBounds(974, 122, 100, 50);
			showVideoPlayerButton.setVisible(false);
			getContentPane().add(showVideoPlayerButton);
		}
	}
	
	public void startComboBoxes()
	{
		comboBoxInteractions = new ComboBox(frameInteractionAssociation.retrieveInteractionsAsArray());
		String [] interactionNames = frameInteractionAssociation.retrieveAllInteractionNames();
		Utils.feedComboBox(comboBoxInteractions, interactionNames);
		comboBoxInteractions.setBounds(258, 7, 276, 27);
		getContentPane().add(comboBoxInteractions);
	}
	
	public void startLabels()
	{
		JLabel lblInteractionType = new JLabel("Interaction Type");
		lblInteractionType.setBounds(141, 11, 139, 16);
		getContentPane().add(lblInteractionType);
		
		startAuxiliaryLabels();
		
		startFrameLabels();
	}

	/**
	 * 
	 */
	public void startAuxiliaryLabels() {
		
		{
			lblPreviousFrame = new JLabel("Frame Before Interaction");
			lblPreviousFrame.setBounds(108, 265, 153, 16);
			getContentPane().add(lblPreviousFrame);
		}
		
		{
			lblInteractionFrame = new JLabel("Interaction Frame");
			lblInteractionFrame.setBounds(419, 265, 110, 16);
			getContentPane().add(lblInteractionFrame);
		}
		
		{
			lblAfterInteraction = new JLabel("Frame After Interaction");
			lblAfterInteraction.setBounds(710, 265, 153, 16);
			getContentPane().add(lblAfterInteraction);
		}
	}

	/**
	 * 
	 */
	public void startFrameLabels() {
		frameLabel = new FrameLabel[3];
		
		frameLabel[0] = new FrameLabel(27, 47, 300, 200);
		frameLabel[1] = new FrameLabel(337, 47, 300, 200);
		frameLabel[2] = new FrameLabel(647, 47, 300, 200);
		
		getContentPane().add(frameLabel[0]);
		getContentPane().add(frameLabel[1]);
		getContentPane().add(frameLabel[2]);
	}
	
	public static void main(String [] args) throws FileNotFoundException, IOException, ParseException
	{
		ShowFrames vp = new ShowFrames();
	}
}