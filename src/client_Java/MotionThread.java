import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MotionThread extends Thread{
	private ClientMonitor monitor;
	private DataInputStream inFromMotionServer;
	private DataOutputStream toMotionServer;
	private ServerConnections connect;
	private Socket motionSocket;
	private int ConnectionId;
	private byte[] receivedInfo;
	private String s;

	//Constructor with reference to Clientmonitor, id for camera being handled (1 or 2)
	//and a ServerConnections containing socket information.
	public MotionThread(ClientMonitor m, int connectionId, ServerConnections c){
		this.connect = c;
		this.monitor = m;
		this.ConnectionId = connectionId;
	}

	public void run(){
		try{
			//Runs as long camera is connected
			while(monitor.IsConnected(ConnectionId)){
				motionSocket = connect.getSocket();
				inFromMotionServer = new DataInputStream(motionSocket.getInputStream());	
				toMotionServer = new DataOutputStream(motionSocket.getOutputStream());

				//Read from stream the signal sent that it's motion time, m for motion.
				receivedInfo = new byte[10];

				inFromMotionServer.read(receivedInfo,0,10);
				s = new String(receivedInfo);
				if(monitor.getAutoDisplay() && s.startsWith("m")){
					monitor.setDisplayMode(Constants.DISPLAYMODE_MOVIE);
					monitor.setModeChanged();
				}
			}
			//Sends pause command to motionServer, flushes stream, closes socket and dies.
			toMotionServer.writeBytes("c");
			toMotionServer.flush();
			motionSocket.close();

		}catch(IOException e){
			e.printStackTrace();
		}
	}
}
