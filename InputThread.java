import java.io.*;
import java.net.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InputThread extends Thread {
	private ClientMonitor monitor;
	private DataInputStream inFromServer;
	private ServerConnections connect;
	private Socket InputSocket;
	private int cameraId;
	private boolean first;
	private long diff;
	private int bytesReadTotal;
	private int bytesRead;
	
	private Matcher matcher;
	private int imgsize;
	private long timestamp;
	byte[] header;
	byte[] imgdata;

	//Constructor containing references to Clientmonitor, id for the camera being handled (1 or 2) 
	//and a ServerConnections containing the socket information.
	public InputThread(ClientMonitor m, int connectionId, ServerConnections c) {
		this.connect = c;
		this.monitor = m;
		this.cameraId = connectionId;
		//Used for time "synchronization" at begining.
		first = true;
		diff = 0;
	}

	public void run() {
		// Runs as long as the connection is up.
		while (monitor.IsConnected(cameraId)) {
			try {
				//Fetch socket from ServerConnections.
				InputSocket = connect.getSocket();
				inFromServer = new DataInputStream(InputSocket.getInputStream());

				/* READ HEADER */
				bytesReadTotal = 0;
				bytesRead = 0;
				header = new byte[30];

				// Reading from stream until predetermined size is received.
				while (bytesReadTotal < 30) {
					bytesRead = inFromServer.read(header, bytesReadTotal, 30 - bytesReadTotal);

					if (bytesRead < 0) {
						// IF DEBUG System.out.println("Error reading bytesRead,
						// header");
					} else {
						bytesReadTotal += bytesRead;
					}
				}

				/* EXTRACT INFORMATION */
				// Create a matcher object for finding image size and timestamp.
				matcher = Pattern.compile("\\d+").matcher(new String(header));

				// Extract imgsize and timestamp.
				matcher.find();
				imgsize = Integer.valueOf(matcher.group());
				matcher.find();
				timestamp = Long.valueOf(matcher.group());

				//Only done once when thread started, "synchronizes" time between camera and client.
				if (first) {
					diff = (System.currentTimeMillis() - timestamp / 1000000);
					first = false;
				}

				//Adjusts timestam received to match client clock.
				timestamp /= 1000000;
				timestamp += diff;

				/* READ PICTURE */
				// With knowledge of image size, read image data from stream
				// until image is completely received
				bytesReadTotal = 0;
				bytesRead = 0;
				imgdata = new byte[imgsize];

				while (bytesReadTotal < imgsize) {
					bytesRead = inFromServer.read(imgdata, bytesReadTotal, imgsize - bytesReadTotal);

					if (bytesRead < 0) {
						// IF DEBUG System.out.println("Error reading bytesRead,
						// imgdata");
					} else {
						bytesReadTotal += bytesRead;
					}
				}

				// Create an ImageData object containing image and timestamp and
				// send to monitor.
				ImagePack a = new ImagePack(imgdata, timestamp, cameraId);
				monitor.putImage(a);
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try{
			inFromServer.close();
			connect.close();
		}catch(IOException e){
			e.printStackTrace();
		}
			
	}
}
