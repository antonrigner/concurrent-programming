import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class OutputThread extends Thread{
	private int cameraId;
	private ClientMonitor monitor;
	private ServerConnections connect;
	private DataOutputStream OutFromClient; 
	private Socket OutputSocket;
	private int mode;

	//Constructor with reference to Clientmonitor, id for the camera being handled (1 or 2) and 
	//a ServerConnections containing the socket information.
	public OutputThread(ClientMonitor m, int cameraId, ServerConnections c) {
		this.cameraId = cameraId;
		this.monitor = m;
		connect = c;
		mode = Constants.DISPLAYMODE_IDLE;
	}

	public void run() {
		//Run while connection is up.
		while(monitor.IsConnected(cameraId)) {

			try{
				//Fetch socket from Serverconnections
				OutputSocket = connect.getSocket();
				OutFromClient = new DataOutputStream(OutputSocket.getOutputStream());

				//Blocks until mode changes or a disconnect occurs.
				mode = monitor.waitForModeChange(cameraId);
	
				//Based on mode received from waitForModeChange, a signal is sent to the camera.
				switch(mode){
				case Constants.DISPLAYMODE_IDLE:
					OutFromClient.writeBytes("i");
					OutFromClient.flush();
					break;

				case Constants.DISPLAYMODE_MOVIE:
					OutFromClient.writeBytes("m");
					OutFromClient.flush();
					break;
				}	

			}catch( IOException e){
				e.printStackTrace();
			}	
		}	
		try{
			//Cleares the stream. sents turn off command to camera, closes socket and dies
			OutFromClient.writeBytes("c");
			OutFromClient.flush();
			connect.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
}