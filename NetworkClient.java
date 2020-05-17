import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class NetworkClient extends Thread{
	public int port;
	public DatagramSocket socket;
	public InetAddress ip;
	public NetworkClient(String ipAdress) {
		this.port = 2000;
		try {
			this.socket = new DatagramSocket();
			this.ip = InetAddress.getByName(ipAdress);
		} catch (SocketException e) {
			e.printStackTrace();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	public String waitForResponse() {
		byte[] data = new byte[1024];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		
		//Socket.receive wartet solange bis der Server antwortet(Hehe... UDP...)
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		String message = new String(packet.getData());
		System.out.println("Server>" + message);
		return message.trim();
		
	}
	public void sendPacket(byte[] data) {
		DatagramPacket packet = new DatagramPacket(data, data.length, ip, this.port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
