

import java.awt.*;
import javax.swing.*;

public class InfoPanel extends JPanel {

	private JLabel delayLabel;

	public InfoPanel(){
		super();
		setLayout(new GridLayout(2,2));
		delayLabel = new JLabel("");
		add(delayLabel);
	}

	//Updates the delay label.
	public void setDelay(int delay){
		delayLabel.setText("Delay [ms] : " + String.valueOf(delay));
	}
}
