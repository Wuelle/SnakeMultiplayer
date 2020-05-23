import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Server extends JPanel implements Runnable{
	private DatagramSocket ServerSocket;
	private String message;
	private byte[] data;
	private DatagramPacket LoginPacket;
	public boolean isRunning;
	
	public Random rand = new Random();
	
	public int players = 0;
	public int[][] player_positions;
	public String[] player_usernames;
	public int[] player_ports;
	public boolean[] player_dead;
	public InetAddress[] player_ips;
	private Thread thread;
	
	public int dir;
	public final int EMPTY = 0;
	public final int WALL = -1;
	public int UP = 2;
	public int DOWN = 3;
	public int LEFT = 1;
	public int RIGHT = 0;
	
	public final int startDelay = 1000;
	public final int delayReduce = 100;
	public final int minDelay = 100;
	public int delay = startDelay;
	public boolean gameOver = false;

	//Commandlline arguments
	public static int mapsize;
	public static int maxPlayers;
	

	
	//map
	public static int[][] grid;
	
	public Server() {
		try {
			this.ServerSocket = new DatagramSocket(2000);
		}catch(BindException e) {
			System.out.println("Port is already in use, are two Server instances running?");
		} 
		catch (SocketException e) {
			e.printStackTrace();
		} 
		
	}
	
	
	public void resetMap() {
		grid = new int[mapsize][mapsize];
		for(int x = 0; x < mapsize; x++) {
			for(int y = 0; y < mapsize; y++) {
				if(x == 0  || y == 0 || x == mapsize-1 || y == mapsize-1) {
					grid[x][y] = WALL;
				}
				else {
					grid[x][y] = EMPTY;
				}
			}
		}
	}
	
	public void PurgeMap(int id) {
		for(int i = 0; i < mapsize; i++) {
			for(int j = 0; j < mapsize; j++) {
				if (grid[i][j] == id) {
					grid[i][j] = EMPTY;
				}
			}
		}
	}
	
	public void sendPacket(byte[] data, InetAddress ip, int port, DatagramSocket socket) {
		DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] getResponseData() {
		//Response hat das format port:Move-port:Move...
		String response = "";
		for(int player = 0; player < maxPlayers; player++) {
			if (player_dead[player]) {
				response += player_usernames[player] + ":" + "dead";
			}
			else {
				response += player_usernames[player] + ":" + Arrays.toString(player_positions[player]);
			}
			
			if (player != maxPlayers-1) {
				response += "-";
			}
			
		}
		return response.getBytes();
	}
	
	public int[][] pickStartPositions(int n) {
		//spieler sollten nicht auf einem spot starten aber java ist eine störrische kuh also pffffffffffff
		int[][] positions = new int[n][2];
		for(int player_ix = 0; player_ix < n; player_ix++) {
			int x = rand.nextInt(mapsize-2)+1;
			int y = rand.nextInt(mapsize-2)+1;
			
			positions[player_ix][0] = x;
			positions[player_ix][1] = y;
			
			grid[x][y] = player_ix+1;
			

		}
		return positions;
	}
	
	public String waitForResponse(DatagramSocket socket) {
		byte[] data = new byte[1024];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		
		//Socket.receive wartet solange bis der Client antwortet(Hehe... UDP...)
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		String message = new String(packet.getData());
		System.out.println("Client>" + message);
		return message.trim();
		
	}



	public static boolean everyoneDead(boolean[] array)
	{
	    for(boolean b : array) if(!b) return false;
	    return true;
	}


	public void run() {
		while (true) {
			//Reset the whole Gamestate
			players = 0;
			gameOver = false;
			player_usernames = new String[maxPlayers];
			player_ports = new int[maxPlayers];
			player_ips = new InetAddress[maxPlayers];;
			player_dead = new boolean[maxPlayers];
			delay = startDelay;
			Arrays.fill(player_dead, false);
			resetMap();
			
			//Wait for enough players to arrive(Nr specified in args)
			for(int x = 0; x < maxPlayers; x++) {
				System.out.println("Waiting for " + String.valueOf(maxPlayers-players) + " more Players...");
				byte[] data = new byte[1024];
				LoginPacket = new DatagramPacket(data, data.length);
				try {
					ServerSocket.receive(LoginPacket);
					message = new String(LoginPacket.getData()).trim();
					System.out.println(message + " just logged in from " + LoginPacket.getAddress().toString() + "[" + String.valueOf(LoginPacket.getPort()) + "]");
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				sendPacket((message + ":" + String.valueOf(mapsize) + ":" + String.valueOf(maxPlayers)).getBytes(), LoginPacket.getAddress(), LoginPacket.getPort(), ServerSocket);
				players += 1;

				player_ips[x] = LoginPacket.getAddress();
				player_ports[x] = LoginPacket.getPort();
				player_usernames[x] = message;
			}
			
			//Start the actual game
			System.out.println("All " + String.valueOf(maxPlayers) + " Players entered, may the best survive!");
			
			player_positions = pickStartPositions(maxPlayers);
			while(!gameOver){
				data = getResponseData();
				
				for (int x = 0; x < maxPlayers; x++) {
					sendPacket(data, player_ips[x], player_ports[x], ServerSocket);
					message = waitForResponse(ServerSocket);
					String[] chunks = message.split(":");
					if(!player_dead[x]) {
						//test if the response is actually from the expected player
						if(chunks[0].equals(player_usernames[x])) {
							
							dir = Integer.parseInt(chunks[1]);
							if (dir == UP) {
								player_positions[x][1] -= 1;
							}
							else if (dir == DOWN) {
								player_positions[x][1] += 1;
							}
							else if (dir == LEFT) {
								player_positions[x][0] -= 1;
							}
							else if (dir == RIGHT) {
								player_positions[x][0] += 1;
							}
							
							//update the serverside gamestate
							int c = grid[player_positions[x][0]][player_positions[x][1]];
							System.out.println("Player is standing on " + String.valueOf(c));
							
							if (c == EMPTY) {
								grid[player_positions[x][0]][player_positions[x][1]] = x+1;
							}
							else {
								player_dead[x] = true;
								
								PurgeMap(x+1);
								
								System.out.println(player_usernames[x] + " just died!");
								if(everyoneDead(player_dead)) {
									gameOver = true;
								}
								
							}
							
						}
						else {
							System.out.println("Received packet from " + chunks[0] + " but expected " + player_usernames[x]);
						}
					}
					
				}
				
				try {
					//the game starts off slowly but speeds up over time
					Thread.sleep(delay);
					if(delay > minDelay) {
						delay -= delayReduce;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		
	}
	
	public synchronized void start() {
		if (isRunning)
			return;
		System.out.println("Start");
		isRunning = true;
		grid = new int[mapsize][mapsize];
		
		JFrame frame = new JFrame();
		JButton btn_shutdown = new JButton("Stop Server");
		frame.add(btn_shutdown);
		btn_shutdown.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e)
		    {
		    	System.out.println("Manually stopped server");
		    	frame.dispose();
		    	System.exit(1);

		    }
		});
		frame.setResizable(false);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null); // Frame zentrieren
		frame.setVisible(true);
		frame.setFocusable(true);
		frame.setTitle("Server");
		
		Dimension dimension = new Dimension(100, 100);
		frame.setPreferredSize(dimension);
		frame.setMinimumSize(dimension);
		frame.setMaximumSize(dimension);
		
		
		
		frame.validate();
		
		//Spiel in neuem Thread starten
		thread = new Thread(this);
		thread.start();
		
		
		
	}
	
	public static void main(String[] args) {	
		Server server = new Server();
		
		//Commandline arguments
		mapsize = Integer.parseInt(args[0]);
		maxPlayers = Integer.parseInt(args[1]);
		
		
		if(maxPlayers > 11) {
			System.out.println("Only eleven colors are supported. If you use more than 11 players, duplicate colors will occur");
		}
	
		server.start();
	}

}
