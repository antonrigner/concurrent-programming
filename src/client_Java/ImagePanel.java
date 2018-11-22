
import java.awt.*;
import javax.swing.*;

/**

 * ImagePanel that contains the pictures

 */

public class ImagePanel extends JPanel {

	ImageIcon icon;

	public ImagePanel() {
		super();
		this.
		initialize();
	}

	private void initialize() {
		this.setLayout(new BorderLayout());
		icon = new ImageIcon();
	}

	//Creates an image from the image bytes.
	public Image prepare(byte[] data) {

		if (data == null) return null;
		Image image = getToolkit().createImage(data);
		Image scaledImage = image.getScaledInstance(400, 400, Image.SCALE_SMOOTH);
		getToolkit().prepareImage(scaledImage, -1, -1, null);
		return scaledImage;
	}

	//Refreshes the image.
	public void refresh(Image image) {
		if (image == null) {
			return;
		}

		if(isShowing()) { 
			icon.setImage(image);
			icon.paintIcon(this, this.getGraphics(), 0, 0);
		}
	}
}
