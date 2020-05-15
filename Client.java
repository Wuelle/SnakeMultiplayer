import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class Client extends JPanel implements Runnable, KeyListener{
	private InetAddress ip;
	private DatagramSocket socket;
	private int port;
	public static JFrame frame; 
	private boolean isRunning = false;
	private String login_id;
	public boolean logged_in;
	public String response;
	public static int mapsize;
	public static int tileSize = 16;
	public static final int WALL = 1;
	public static int WIDTH;
	public static int HEIGHT;
	
	// Das Spielfeld
	public static int[][] grid;
	
	Random rand = new Random();
	
	public Client(String ip) {
		
		try {
			this.socket = new DatagramSocket();
			this.ip = InetAddress.getByName(ip);
		} catch (SocketException e) {
			e.printStackTrace();
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		
		//Die beiden Spieler schicken Daten über unterschiedliche ports
		this.port = 2000;
		
		login();
		
		//Spielkram
		Dimension dimension = new Dimension(WIDTH, HEIGHT);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setMaximumSize(dimension);
		addKeyListener(this);
		
		resetMap();
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
	
	public synchronized void login() {
		login_id = String.valueOf(rand.nextInt(5000));
		sendPacket(login_id.getBytes());
		logged_in = false;
		while (!logged_in) {
			response = waitForResponse();
			if (response.substring(0, login_id.length()).compareTo(login_id) == 0) {
				this.port = Integer.parseInt(response.substring(login_id.length(), login_id.length()+4));
				mapsize = Integer.parseInt(response.substring(login_id.length()+4));

				logged_in = true;
				WIDTH = tileSize*mapsize;
				HEIGHT = tileSize*mapsize;
			}	
		}
	}
	
	// III. Alles Zeichnen: render()
		public void paint(Graphics g) {
			int c;
			g.setColor(Color.white); // weissen Hintergrund malen
			g.fillRect(0, 0, WIDTH, HEIGHT);

			for (int y = 0; y < mapsize; y++) {
				for (int x = 0; x < mapsize; x++) {
					c = grid[x][y];
					if (c == WALL) {
						g.setColor(Color.black);
						g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);	
					}
			
					else if(c == 2) {
						g.setColor(Color.green);
						g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
					}
			
					else if(c == 3) {
						g.setColor(Color.red);
						g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);
					}
				}
		
			}	
			g.dispose();
		}
	
	public synchronized void start() {
		if (isRunning)
			return;
		
		isRunning = true;
	
		//Spiel in neuem Thread starten
		Thread thread = new Thread(this);
		thread.start();
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
	
	public static void main(String[] args) {
		
		
		frame = new JFrame();
		String ip = JOptionPane.showInputDialog("Server IP:");
		System.out.println(ip);
		Client client = new Client(ip);
		frame.add(client);
		frame.setResizable(false);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null); // Frame zentrieren
		frame.setVisible(true);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		
		
		
		frame.validate();
		client.start();
	}

	@Override
	public void run() {
		System.out.println("ich renne!");
		sendPacket("hallo".getBytes());
		
	}
	
	public synchronized void stop() {
		if (!isRunning)
			return;
		isRunning = false;
	}
	
	//Spielerinput
	@Override
	public void keyPressed(KeyEvent e) {
		//Spieler Steuerung
		/*
		if(e.getKeyCode() == KeyEvent.VK_D && socketClient.dir!=1) socketClient.dir = 0;
		else if(e.getKeyCode() == KeyEvent.VK_A && socketClient.dir!=0) socketClient.dir = 1;
		else if(e.getKeyCode() == KeyEvent.VK_W && socketClient.dir!=3) socketClient.dir = 2;
		else if(e.getKeyCode() == KeyEvent.VK_S && socketClient.dir!=2) socketClient.dir = 3;
		*/
		
		
	

	}

	@Override
	public void keyReleased(KeyEvent e) {
		// Pfeiltasten losgelassen
		// * ... */
	}

	
	// Unnötig (darf man trotzdem nicht löschen)
	@Override
	public void keyTyped(KeyEvent e) {
		;
		
	}

	
	
}
