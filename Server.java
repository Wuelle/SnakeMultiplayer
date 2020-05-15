import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Server implements Runnable{
	private DatagramSocket LoginSocket;
	private String message;
	private byte[] data;
	private DatagramPacket LoginPacket;
	public boolean isRunning;
	public static int mapsize;
	public static int maxPlayers;
	public int portCounter = 2001;

	
	// Das Spielfeld
	public static int[][] grid = new int[mapsize][mapsize];
	public static int[][] gridOrig = new int[mapsize][mapsize];
	
	public Server() {
		
		//Jeder Spieler verbindet sich zuerst mit dem loginport und bekommt dann einen individuellen zugewiesen
		try {
			this.LoginSocket = new DatagramSocket(2000);
		}catch(BindException e) {
			System.out.println("Port wird bereits benutzt, läuft der Host doppelt?");
		} 
		catch (SocketException e) {
			e.printStackTrace();
		} 
		
	}
	
	private int step() {
		return 0;
		
	}
	
	public void resetMap() {
		grid = new int[mapsize][mapsize];
		for(int x = 0; x < mapsize; x++) {
			for(int y = 0; y < mapsize; y++) {
				if(x == 0  || y == 0 || x == mapsize-1 || y == mapsize-1) {
					grid[x][y] = 1;
				}
				else {
					grid[x][y] = 0;
				}
			}
		}
	}
	
	public byte[] encodeData() {
		return data;
			
	}
	
	
	public void sendPacket(byte[] data, InetAddress ip, int port, DatagramSocket socket) {
		DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	
	public void run() {
		byte[] data = new byte[1024];
		LoginPacket = new DatagramPacket(data, data.length);
		try {
			LoginSocket.receive(LoginPacket);
			message = new String(LoginPacket.getData()).trim();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(message);
		System.out.println("Loginpacket empfangen!");
		
		sendPacket((message + String.valueOf(portCounter) + String.valueOf(mapsize)).getBytes(), LoginPacket.getAddress(), LoginPacket.getPort(), LoginSocket);
	}
	
	public synchronized void start() {
		System.out.println("Server start");
		if (isRunning)
			return;
		
		isRunning = true;
		
		//Spiel in neuem Thread starten
		Thread thread = new Thread(this);
		thread.start();
		
		
		
	}
	
	public static void main(String[] args) {
			
		Server server = new Server();
		
		//Commandline arguments
		mapsize = Integer.parseInt(args[0]);
		maxPlayers = Integer.parseInt(args[1]);
		
		System.out.println("Start");
		server.start();
	}

}
