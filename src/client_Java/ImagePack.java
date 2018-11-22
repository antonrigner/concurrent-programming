
public class ImagePack {
	private byte[] image;
	private long timestamp;
	private int cameraId;

	//Create an ImageData object containing image bytes, timestamp and camera ID.
	public ImagePack(byte[] image, long timestamp, int cameraId){
		this.image = image;
		//Dividing by a million to go from nanoseconds to milliseconds.  
		this.timestamp = timestamp;
		this.cameraId = cameraId;
	}

	//Returns image bytes.
	public byte[] getImage(){
		return image;
	}

	//Returns timestamp.
	public long getTimestamp(){
		return timestamp;
	}

	//Returns cameraId;
	public int getCameraId(){
		return cameraId;
	}

}
