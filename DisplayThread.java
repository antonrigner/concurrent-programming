
public class DisplayThread extends Thread {

	private ClientMonitor monitor;
	private GUI gui;
	private int synchMode;
	private int displayMode;


	// gets image from monitor
	// decide synch/asynch mode
	// send Image and Mode to GUI

	public DisplayThread(ClientMonitor m, GUI gui){
		monitor = m;
		this.gui = gui;
	}

	public void run(){

		while(true){

			//Gets imagePack from from monitor containing imageData, timestamp and camera ID.
			ImagePack imagePack = monitor.getImagePack();
			
			int id = imagePack.getCameraId();			
			byte[] data = imagePack.getImage();		
			int delay = (int) monitor.getDelay(id);
		
			displayMode = monitor.getDisplayMode();
			synchMode = monitor.getSynchMode();			
			
			//Updates gui with the information above.
			gui.displayImage(id , data, delay, synchMode, displayMode);
		}
	}

}
