import java.io.IOException;
import java.net.Socket;

public class ServerConnections {
	private Socket SCSocket;
	private String adress;
	private int portnbr;
	private int disconnectAttempts;

	//Constructor with adress and port number.
	public ServerConnections(String adress, int portnbr){
		this.adress = adress;
		this.portnbr = portnbr;
		disconnectAttempts = 0;
		connect();

	}

	//Returns the created socket.
	public Socket getSocket(){
		return SCSocket;
	}

	//Connects to the socket indicated by the given adress and port number in the constructor.
	private void connect(){
		try{
			SCSocket = new Socket(adress, portnbr);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	//Closes socket.
	public void close(){
		disconnectAttempts++;
		try{
			if(disconnectAttempts == 2){
			SCSocket.close();
			disconnectAttempts = 0;
			}
			
		}catch(IOException e){
			e.printStackTrace();
		}

	}
}
