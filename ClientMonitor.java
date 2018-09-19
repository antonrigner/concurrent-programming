import java.util.LinkedList;


import javax.swing.plaf.synth.SynthSpinnerUI;
public class ClientMonitor {
	private LinkedList<ImagePack> images1;
	private LinkedList<ImagePack> images2;

	private boolean modeChanged; 
	private boolean autoDisplayMode;
	private boolean autoSynchMode;
	private boolean[] connectStatus;

	private int synchMode;
	private int displayMode, newDisplayMode;
	private int port1, port2, port3, port4;
	private int nbrOfOutputThreads;
	private int outputThreadNotified;
	private int mainSwitch;
	private int asynchCount;

	private double delay1, delay2;

	private long prevTimestamp;
	private long dT1, dT2;
	private long waitTime;
	private long firstTimestamp1, firstTimestamp2, secondTimestamp1, secondTimestamp2;	
	private long prevClientTimeLastSend;
	private long waitTimeMargin;
	
	private String name1, name2, name3, name4;

	private ServerConnections camera1;
	private ServerConnections camera2;
	private ServerConnections motion1;
	private ServerConnections motion2;

	private final static double THRESHOLD = 200;	


	public ClientMonitor(String name1, String name2, String name3, String name4, int port1, int port2, int port3, int port4){
		//Two lists, one for each camera;
		images1 = new LinkedList<ImagePack>();
		images2 = new LinkedList<ImagePack>();

		//Start in asynch mode.
		synchMode = Constants.SYNCHMODE_ASYNCH;

		//Start in idle mode.
		displayMode = Constants.DISPLAYMODE_IDLE;

		//Option for allowing automatic switching between movie and idle, automatic switching when true.
		autoDisplayMode = false;
		autoSynchMode = false;

		//True when motion detected by one camera.
		modeChanged = false;

		//Current connections status for the cameras, true if connected. Starts as false for both.
		connectStatus = new boolean[2];
		connectStatus[0] = connectStatus[1] = false;

		//Names and port numbers.
		this.name1 = name1;
		this.name2 = name2;
		this.name3 = name3;
		this.name4 = name4;
		this.port1 = port1;
		this.port2 = port2;
		this.port3 = port3;
		this.port4 = port4;

		nbrOfOutputThreads = 0;
		outputThreadNotified = 0;

		prevTimestamp = 0;

		mainSwitch = 1;		

		asynchCount = 0;
		delay1 = delay2 = 0;
		prevClientTimeLastSend = 0;
		
		waitTimeMargin = 10;
		newDisplayMode = 0;

	}
	//Connects the camera associated with id. Starts all the relevant threads.
	public synchronized void Connect(int id){

		//Creates socket and related threads for camera 1 when connected.
		if(id == 1){
			images1.clear();
			//Camera socket.
			camera1 = new ServerConnections(name1, port1);
			//MotionServer socket.
			motion1 = new ServerConnections(name3, port3);
			new Thread(new InputThread(this,id,camera1)).start();
			new Thread(new OutputThread(this,id,camera1)).start();
			new Thread(new MotionThread(this, id, motion1)).start();
			connectStatus[0] = true;

			//If no cameras previously connected, sets initiation time for client.
		}else{
			images2.clear();
			//Camera socket.
			camera2 = new ServerConnections(name2, port2);
			//MotionServer socket.
			motion2 = new ServerConnections(name4, port4);
			new Thread(new InputThread(this,id, camera2)).start();
			new Thread(new OutputThread(this,id, camera2)).start();
			new Thread(new MotionThread(this, id, motion2)).start();
			connectStatus[1] = true;
		}
		nbrOfOutputThreads++;
		notifyAll();
	}

	//Disconnects the camera associated with id. Clears the associated image list for abrupt stop.
	public synchronized void disConnect(int id){
		if(id == 1){
			images1.clear();
			connectStatus[0] = false;
		}else {
			images2.clear();
			connectStatus[1] = false;
		}
		nbrOfOutputThreads--;
		notifyAll();
	}

	//Returns the current connection status for the camera associated with id.
	public synchronized boolean IsConnected(int id){
		return connectStatus[id-1];
	}

	//Adds an image from InputThread in the correct list according to the camera which took the picture.
	public synchronized void putImage(ImagePack imagePack) {
		int id = imagePack.getCameraId();
		if(id == 1){

			images1.add(imagePack);

		}else{
			images2.add(imagePack);
		}	
		notifyAll();
	}

	//Returns image from one of the two cameras according to the current synch mode active.
	public synchronized ImagePack getImagePack(){		

		ImagePack imagePack = null;
		dT1 = 0; 		//Timestamp diffenrence between two first images in images1.
		dT2= 0;			//Timestamp diffenrence between two first images in images2.
		waitTime = 0; 	//Time to wait in synchMode before sending new image. 

		firstTimestamp1 = Long.MAX_VALUE; //Timestamp for the first image in images1.
		firstTimestamp2 = Long.MAX_VALUE; //Timestamp for the first image in images2.

		try{
			while(images1.isEmpty() && images2.isEmpty()) wait();
		}catch(Exception e){
			e.printStackTrace();
		}

		//If images1 is contains two or more images, calculates dT1 and the delay for the first image.
		if(images1.size() >= 2){
			firstTimestamp1 = images1.getFirst().getTimestamp();
			secondTimestamp1 = images1.get(1).getTimestamp();
			dT1 = secondTimestamp1 - firstTimestamp1;
			delay1 = (double) System.currentTimeMillis() - firstTimestamp1;

		//If images1 contains one image.
		}else if(!images1.isEmpty()){	
			firstTimestamp1 = images1.getFirst().getTimestamp();
			dT1 = 0;
			delay1 = (double) System.currentTimeMillis() - firstTimestamp1;
		}

		//If images2 is contains two or more images, calculates dT2 and the delay for the first image.
		if(images2.size() >= 2){
			firstTimestamp2 = images2.getFirst().getTimestamp();
			secondTimestamp2 = images2.get(1).getTimestamp();
			dT2 = secondTimestamp2 - firstTimestamp2;			
			delay2 = (double) System.currentTimeMillis() - firstTimestamp2;

		//If images2 contains one image.
		}else if(!images2.isEmpty()){
			firstTimestamp2 = images2.getFirst().getTimestamp();
			dT2 = 0;
			delay2 = (double) System.currentTimeMillis() - firstTimestamp2;
		}

		//If both cameras meets the synchronization criteria.
		if(dT1 < THRESHOLD && dT2 < THRESHOLD){
			if(autoSynchMode){
				synchMode = Constants.SYNCHMODE_SYNCH;
				asynchCount = 0;
			}

			//Image from camera 1 is the oldest, show first
			if(firstTimestamp1 < firstTimestamp2){ 				
				waitTime = firstTimestamp1 - prevTimestamp; 				
				if(prevTimestamp == 0) waitTime = 0; //first image should be displayed immediately				
				imagePack = images1.poll();

			//Image from camera 2 is the oldest, show first
			} else {				
				waitTime = firstTimestamp2 - prevTimestamp;				
				if(prevTimestamp == 0) waitTime = 0;			
				imagePack = images2.poll();
			}

		//If any of the two time differences are not below the threshold, allow five succeeding images 
		//and then switch to asynch mode.
		} else {		
			if(firstTimestamp1 < firstTimestamp2){
				imagePack = images1.poll();
			}else{
				imagePack = images2.poll();
			}

			if(autoSynchMode && synchMode == Constants.SYNCHMODE_SYNCH){
				asynchCount++;
				if(asynchCount >= 5){		
					synchMode = Constants.SYNCHMODE_ASYNCH;
					images1.clear();
					images2.clear();
					asynchCount = 0;
				}				
			}
		}

		//If in synchronous mode wait waitTime.
		if(synchMode == Constants.SYNCHMODE_SYNCH){	
			try {
				while( (System.currentTimeMillis() - prevClientTimeLastSend) < waitTime-waitTimeMargin){
					wait( waitTime - (System.currentTimeMillis() - prevClientTimeLastSend));
				}

			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		if(imagePack.getCameraId() == 1){
			prevTimestamp = firstTimestamp1;
			prevClientTimeLastSend = System.currentTimeMillis(); 
		}else{
			prevTimestamp = firstTimestamp2;
			prevClientTimeLastSend = System.currentTimeMillis(); 
		}		

		notifyAll();
		return imagePack;
	}

	//Returns delay from camera with ID id.
	public synchronized double getDelay(int id){
		if(id == 1){
			return delay1;
		}else{
			return delay2;
		}	
	}

	//Returns the current mode reflecting the way the images are displayed, synched or asynch.
	public synchronized int getSynchMode(){
		return synchMode;

	}


	//Set true Auto synch
	public synchronized void setAutoSynchMode(boolean mode){
		autoSynchMode = mode;
		asynchCount = 0; //reset asynchCount
		notifyAll();

	}

	//Returns true if Auto synch
	public synchronized boolean getAutoSynchMode(){
		return autoSynchMode;

	}

	//Set new mode for displaing images, movie or idle.
	public synchronized void setDisplayMode(int mode){
		notifyAll();
		displayMode = mode;
		images1.clear();
		images2.clear();
	}

	//Returns the current mode for displaing images, movie or idle.
	public synchronized int getDisplayMode(){ 
		return newDisplayMode;				
	}

	//Sets motionDetected as true when MotionInput receives information about movement or a connect/disconnect occurs.
	public synchronized void setModeChanged(){
		modeChanged = true;
		notifyAll();
	}

	//Blocks threads until modeChange is true and the thread with the correct mySwitch access method.
	//mySwitch is used for assuring alternate access when two cameras are connected.
	public synchronized int waitForModeChange(int mySwitch){
		try{
			boolean camOne = connectStatus[0];
			boolean camTwo = connectStatus[1];

			//If only one camera is connected.
			//Cam 1 connected
			if((camOne && !camTwo) || (!camOne && camTwo)){
				while(!modeChanged){
					wait();
				}

			//Cam 2 connected
			}else{
				while((!modeChanged) || mySwitch != mainSwitch){
					wait();
				}			
			}		
		}catch(InterruptedException e){
			e.printStackTrace();
		}

		if(mySwitch ==1){
			mainSwitch = 2;
		}else{
			mainSwitch = 1;
		}

		//When both outputThreads has been notified, reset modeChange.
		outputThreadNotified++;
		if(outputThreadNotified == 1) newDisplayMode = displayMode;
		
		if(outputThreadNotified >= nbrOfOutputThreads){
			modeChanged = false;
			outputThreadNotified = 0;
		}
		return newDisplayMode;
		//return displayMode;
	}

	//Information from GUI if user triggers Automode for movie and idle.
	public synchronized void setAutoDisplay(boolean mode){
		autoDisplayMode = mode;
		notifyAll();
	}

	//Returns true for auto on display mode.
	public synchronized boolean getAutoDisplay(){
		return autoDisplayMode;
	}

	//Sets synch mode as mode
	public synchronized void setSyncMode(int mode){
		synchMode = mode;
		notifyAll();
	}

}