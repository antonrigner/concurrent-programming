

public class Main {
	public static void main(String args[]){
		//Change the x in "argus-x" depending on camera chosen.
		String host1 = "argus-1.student.lth.se"; // camera left
		String host2 = "argus-7.student.lth.se"; // camera right
		String host3 = "argus-1.student.lth.se"; // motion left
		String host4 = "argus-7.student.lth.se"; // motion right
		
		//Choose same port nmbr as whn starting the cameras.
		int port1 = 5002; // camera left
		int port2 = 5002; // camera right
		int port3 = 5003; // motion left
		int port4 = 5003; // motion right
		
		ClientMonitor m = new ClientMonitor( host1, host2, host3, host4, port1, port2, port3, port4);
		GUI gui = new GUI(m);
		DisplayThread displayThread = new DisplayThread(m, gui);
		displayThread.start();
		}
}