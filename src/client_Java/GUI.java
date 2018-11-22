
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import java.awt.Font;



public class GUI {

	private ImagePanel[] imagePanel;
	private InfoPanel[] infoPanel;
	private ClientMonitor monitor;
	private JButton synchButton, aSynchButton, movieButton, idleButton;
	private JToggleButton  autoSynchButton, autoDisplayButton;	
	private JLabel currentSynchModeLabel, currentDisplayModeLabel;

	public GUI(ClientMonitor m){		
		monitor = m;
		imagePanel = new ImagePanel[3];
		infoPanel = new InfoPanel[3];
		initUI();
	}

	//Initiates the GUI
	private void initUI() {
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				JFrame frame = new JFrame();
				Container contentPane = frame.getContentPane();
				contentPane.setBackground(Color.GRAY);
				contentPane.setLayout(new GridBagLayout());

				GridBagConstraints main = new GridBagConstraints();

				JPanel leftPanel = new JPanel();
				leftPanel.setBackground(Color.GRAY);

				main.fill = GridBagConstraints.HORIZONTAL;
				main.fill = GridBagConstraints.VERTICAL;
				main.weightx = 0.2;
				main.gridx = 1;
				main.gridy = 0;
				contentPane.add(leftPanel, main);

				JPanel midPanel = new JPanel();
				main.weightx = 0.4;
				main.gridx = 2;
				contentPane.add(midPanel, main);

				JPanel rightPanel = new JPanel();

				main.weightx = 0.4;
				main.gridx = 3;
				contentPane.add(rightPanel, main);

				leftPanel.setLayout(new GridBagLayout());
				GridBagConstraints cons = new GridBagConstraints();

				midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
				rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

				synchButton = new JButton("SYNCHRONIZED");
				aSynchButton = new JButton("ASYNCHRONIZED");
				autoSynchButton = new JToggleButton("AUTO");

				synchButton.setFont(new Font("Ubuntu", Font.BOLD, 12));
				aSynchButton.setFont(new Font("Ubuntu", Font.BOLD, 12));
				autoSynchButton.setFont(new Font("Ubuntu", Font.BOLD, 12));


				synchButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (synchButton.isEnabled()) {
							monitor.setSyncMode(Constants.SYNCHMODE_SYNCH);
							if(autoSynchButton.isSelected()) autoSynchButton.setSelected(false);
						}
					}
				});

				aSynchButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (aSynchButton.isEnabled()) {
							monitor.setSyncMode(Constants.SYNCHMODE_ASYNCH);
							if(autoSynchButton.isSelected()) autoSynchButton.setSelected(false);
						}
					}
				});

				autoSynchButton.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ev) {						
						if(ev.getStateChange()==ItemEvent.SELECTED){
							monitor.setAutoSynchMode(true);
						}else if(ev.getStateChange()==ItemEvent.DESELECTED){
							monitor.setAutoSynchMode(false);
						}
					}
				});

				idleButton = new JButton("IDLE");
				movieButton = new JButton("MOVIE");
				autoDisplayButton = new JToggleButton("AUTO");

				idleButton.setFont(new Font("Ubuntu", Font.BOLD, 12));
				movieButton.setFont(new Font("Ubuntu", Font.BOLD, 12));
				autoDisplayButton.setFont(new Font("Ubuntu", Font.BOLD, 12));

				idleButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (idleButton.isEnabled()) {
							monitor.setDisplayMode(Constants.DISPLAYMODE_IDLE);
							monitor.setModeChanged();
							if(autoDisplayButton.isSelected()) autoDisplayButton.setSelected(false);
						}				  
					}
				});
				movieButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (movieButton.isEnabled()) {
							monitor.setDisplayMode(Constants.DISPLAYMODE_MOVIE);
							monitor.setModeChanged();
							if(autoDisplayButton.isSelected()) autoDisplayButton.setSelected(false);
						}				  
					}
				});

				autoDisplayButton.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ev) {						
						if(ev.getStateChange()==ItemEvent.SELECTED){
							monitor.setAutoDisplay(true);
						}else if(ev.getStateChange()==ItemEvent.DESELECTED){
							monitor.setAutoDisplay(false);
						}
					}
				});

				
				cons.fill = GridBagConstraints.HORIZONTAL;
				cons.weightx = 1;
				cons.insets = new Insets(2,0,0,0);  //top padding
				cons.gridx = 0;
				cons.gridy = 0;	
				leftPanel.add(synchButton, cons);

				cons.gridy = 1;		
				leftPanel.add(aSynchButton, cons);

				cons.gridy = 2;		
				leftPanel.add(autoSynchButton, cons);

				JLabel currentSynchMode = new JLabel("Current synch mode:", SwingConstants.CENTER);
				currentSynchMode.setFont(new Font("Ubuntu", Font.BOLD, 14));
				cons.insets = new Insets(10,0,0,0);
				cons.gridy = 3;	
				leftPanel.add(currentSynchMode, cons);
				
				currentSynchModeLabel = new JLabel("ASYNCHRONIZED", SwingConstants.CENTER);
				currentSynchModeLabel.setFont(new Font("Ubuntu", Font.BOLD, 14));
				currentSynchModeLabel.setForeground(new Color(0, 0, 0));
				cons.gridy = 4;	
				leftPanel.add(currentSynchModeLabel, cons);								
				
				JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
				separator.setBackground(Color.BLACK);
				separator.setForeground(Color.BLACK);
				cons.insets = new Insets(20,0,0,0);  
				cons.gridy = 5;		
				leftPanel.add(separator, cons);				

				cons.insets = new Insets(20,0,0,0);  
				cons.gridy = 6;	
				leftPanel.add(idleButton, cons);

				cons.insets = new Insets(2,0,0,0);  	
				cons.gridy = 7;						
				leftPanel.add(movieButton, cons);

				cons.gridy = 8;			
				leftPanel.add(autoDisplayButton, cons);		

				JLabel movieMode = new JLabel("Current display mode:", SwingConstants.CENTER);	
				movieMode.setFont(new Font("Ubuntu", Font.BOLD, 14));
				cons.insets = new Insets(10,0,0,0);
				cons.gridy = 9;		
				leftPanel.add(movieMode, cons);	
				
				currentDisplayModeLabel = new JLabel("IDLE", SwingConstants.CENTER);
				currentDisplayModeLabel.setFont(new Font("Ubuntu", Font.BOLD, 14));
				currentDisplayModeLabel.setForeground(new Color(0, 0, 0));
				cons.gridy = 10;	
				leftPanel.add(currentDisplayModeLabel, cons);
				
				JPanel cam1Border = new JPanel();
				cam1Border.setLayout(new BorderLayout());
				cam1Border.setBackground(Color.GRAY);

				JLabel cam1Label = new JLabel("CAMERA 1");
				cam1Label.setFont(new Font("Ubuntu", Font.BOLD, 14));
				cam1Label.setHorizontalAlignment(JLabel.CENTER);
				cam1Border.add(cam1Label, BorderLayout.LINE_START);

				JToggleButton connectCam1 = new JToggleButton("CONNECT");
				connectCam1.setFont(new Font("Ubuntu", Font.BOLD, 12));
				connectCam1.setPreferredSize(new Dimension(120,25));
				connectCam1.setForeground(new Color(9, 163, 50));

				connectCam1.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ev) {
						if(ev.getStateChange()==ItemEvent.SELECTED){
							monitor.Connect(1);
							monitor.setModeChanged();
							connectCam1.setText("Disconnect");
							connectCam1.setForeground(new Color(206, 16, 16));
						} else if(ev.getStateChange()==ItemEvent.DESELECTED){
							monitor.setModeChanged();
							monitor.disConnect(1);
							connectCam1.setText("Connect");
							connectCam1.setForeground(new Color(9, 163, 50));
						}
					}
				});

				cam1Border.add(connectCam1, BorderLayout.LINE_END);

				ImagePanel imagePanel1 = new ImagePanel();

				imagePanel[1] = imagePanel1;

				imagePanel1.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));
				InfoPanel infoPanel1 = new InfoPanel();
				infoPanel1.setDelay(0);
				infoPanel1.setBackground(Color.GRAY);

				infoPanel[1] = infoPanel1;

				imagePanel1.setPreferredSize(new Dimension(400,400));

				midPanel.add(cam1Border);
				midPanel.add(imagePanel1);
				midPanel.add(infoPanel1);

				JPanel cam2Border = new JPanel();
				cam2Border.setLayout(new BorderLayout());

				JLabel cam2Label = new JLabel("CAMERA 2");
				cam2Label.setFont(new Font("Ubuntu", Font.BOLD, 14));
				cam2Label.setHorizontalAlignment(JLabel.CENTER);
				cam2Border.add(cam2Label, BorderLayout.LINE_START);
				cam2Border.setBackground(Color.GRAY);

				JToggleButton connectCam2 = new JToggleButton("CONNECT");
				connectCam2.setFont(new Font("Ubuntu", Font.BOLD, 12));
				connectCam2.setPreferredSize(new Dimension(120,25));
				connectCam2.setForeground(new Color(9, 163, 50));

				connectCam2.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ev) {
						if(ev.getStateChange()==ItemEvent.SELECTED){
							monitor.Connect(2);
							monitor.setModeChanged();
							connectCam2.setText("Disconnect");
							connectCam2.setForeground(new Color(206, 16, 16));

						} else if(ev.getStateChange()==ItemEvent.DESELECTED){
							monitor.setModeChanged();
							monitor.disConnect(2);
							connectCam2.setText("Connect");
							connectCam2.setForeground(new Color(9, 163, 50));
						}
					}
				});

				cam2Border.add(connectCam2, BorderLayout.LINE_END);

				ImagePanel imagePanel2 = new ImagePanel();
				imagePanel2.setBorder(BorderFactory.createMatteBorder(2, 2, 2, 2, Color.BLACK));

				imagePanel[2] = imagePanel2;

				InfoPanel infoPanel2 = new InfoPanel();
				infoPanel2.setDelay(0);
				infoPanel2.setBackground(Color.GRAY);

				infoPanel[2] = infoPanel2;
				imagePanel2.setPreferredSize(new Dimension(400,400));

				rightPanel.add(cam2Border);
				rightPanel.add(imagePanel2);
				rightPanel.add(infoPanel2);

				frame.setPreferredSize(new Dimension(1000,500));
				frame.pack();
				frame.setLocationRelativeTo(null);
				frame.setVisible(true);
				frame.setResizable(false);
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			}
		});
	}


	private class Data {
		public Image image;
		public int delay;
		public int id;

		public Data(int id, Image i, int d) {
			this.id = id;
			image = i;
			delay = d;	
		}
	}

	//Updates the gui with the new image, delay, synch mode and display mode
	public void displayImage(int id, byte[] data, int delay, int synchMode, int displayMode) {


		if(synchMode == Constants.SYNCHMODE_SYNCH){
			currentSynchModeLabel.setText("SYNCHRONIZED");
		}else{
			currentSynchModeLabel.setText("ASYNCHRONIZED");
		}

		if(displayMode == Constants.DISPLAYMODE_IDLE){
			currentDisplayModeLabel.setText("IDLE");
		}else{
			currentDisplayModeLabel.setText("MOVIE");
		}

		SwingWorker<Data,byte[]> sw = new SwingWorker<Data,byte[]>() {

			@Override
			protected Data doInBackground() throws Exception {

				Image image = imagePanel[id].prepare(data);
				return new Data(id, image, delay);
			}

			@Override
			protected void done() {
				try {
					if (get() == null) return;
					imagePanel[get().id].refresh(get().image);
					infoPanel[get().id].setDelay(get().delay);					

				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		};		
		sw.execute();
	}	
}
