import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class Game extends JPanel implements Runnable, KeyListener{
	private static final long serialVersionUID = 1L;
	public static JFrame frame; 
	private boolean isRunning = false;
	public boolean logged_in;
	public String response;
	public static int mapsize;
	public static int tileSize = 16;
	public static final int WALL = -1;
	public static final int EMPTY = 0;
	public static int WIDTH;
	public static int HEIGHT;
	public static int dir;
	public static int maxPlayers;
	public static String username;
	public static String[] usernames;
	public static String ipAdress;
	public static Color[] player_colors = new Color[]{Color.BLUE, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.RED, Color.YELLOW,
			Color.CYAN, Color.GREEN, Color.DARK_GRAY, Color.GRAY, Color.LIGHT_GRAY};
	public String state = "waiting";
	
	
	BufferedImage waiting_img = null;
	
	//player directions
	public int UP = 2;
	public int DOWN = 3;
	public int LEFT = 1;
	public int RIGHT = 0;
	
	public Thread thread;
	public NetworkClient client;
	
	//map
	public static int[][] grid;
	
	Random rand = new Random();
	
	public Game(String ipAdress) {
		client = new NetworkClient(ipAdress);
		client.start();
		
		login();
		resetMap();
		
		//Load and rescale the images
		try 
		{
		    waiting_img = ImageIO.read(new File("C:/Users/Wuelle/eclipse-workspace/SnakeMultiplayer/waiting.png")); 
		    waiting_img = scale(waiting_img, WIDTH, HEIGHT);
		} 
		catch (IOException e) 
		{
		    e.printStackTrace();
		}

		//Window stuff
		Dimension dimension = new Dimension(WIDTH, HEIGHT);
		setPreferredSize(dimension);
		setMinimumSize(dimension);
		setMaximumSize(dimension);
		addKeyListener(this);
	}
	
	public synchronized void login() {
		client.sendPacket(username.getBytes());
		logged_in = false;
		while (!logged_in) {
			response = client.waitForResponse();
			String[] blocks = response.split(":");
			mapsize = Integer.parseInt(blocks[1]);
			maxPlayers = Integer.parseInt(blocks[2]);
			usernames = new String[maxPlayers];

			logged_in = true;
			WIDTH = tileSize*mapsize;
			HEIGHT = tileSize*mapsize;
		}
	}
	
	// III. Alles Zeichnen: render()
		public void paint(Graphics g) {
			if (state == "waiting") {
				g.drawImage(waiting_img, 0, 0, null);
			}
			else {
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
						else if (c != EMPTY && c < 100) {
							g.setColor(player_colors[c-1]);
							g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);	
						}
						//if snake head
						else if (c > 100) {
							//reset the head to a body piece(new head will be created at the next timestep)
							grid[x][y] -= 100;
							
							//Draw username at the heads location
							g.setColor(Color.black);
							g.drawString(usernames[c-101], x * tileSize, y * tileSize);
							
							//draw the usual body
							g.setColor(player_colors[(c-101) % player_colors.length]);
							g.fillRect(x * tileSize, y * tileSize, tileSize, tileSize);	
						}
					}
			
				}	
			}
			g.dispose();
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
	@Override
	public void run() {
		//Window needs to be focused for the keylistener to work
		requestFocus();
		//Wait for the server to gather up enough players for a game
		System.out.println("Waiting for more players...");
		response = client.waitForResponse().replace(" ", "").replace("[", "").replace("]", "");
		state = "playing";
		while(isRunning){
			String[] chunks = response.split("-");
			
			//the chunks always arrive in the same order so chunk_ix can be used as player index
			for(int chunk_ix = 0; chunk_ix < chunks.length; chunk_ix++) {
				String[] data = chunks[chunk_ix].split(":");
				
				//usernames muss eig nicht immer überschrieben werden
				usernames[chunk_ix] = data[0];
				if(!data[1].equals("dead")) {
					String[] coords = data[1].split(",");
					grid[Integer.parseInt(coords[0])][Integer.parseInt(coords[1])] = 100+chunk_ix+1;
				}
				else {
					PurgeMap(chunk_ix+1);
				}
			}
			frame.repaint();
			
			client.sendPacket((username + ":" + String.valueOf(dir)).getBytes());
			response = client.waitForResponse().replace(" ", "").replace("[", "").replace("]", "");
		}
		stop();
		

	}
	
	//Spielerinput
	@Override
	public void keyPressed(KeyEvent e) {
		//WASD
		if(e.getKeyCode() == KeyEvent.VK_D && dir!=LEFT) dir = RIGHT;
		else if(e.getKeyCode() == KeyEvent.VK_A && dir!=RIGHT) dir = LEFT;
		else if(e.getKeyCode() == KeyEvent.VK_W && dir!=DOWN) dir = UP;
		else if(e.getKeyCode() == KeyEvent.VK_S && dir!=UP) dir = DOWN;
		
		//Arrow keys
		if(e.getKeyCode() == KeyEvent.VK_RIGHT && dir!=LEFT) dir = RIGHT;
		else if(e.getKeyCode() == KeyEvent.VK_LEFT && dir!=RIGHT) dir = LEFT;
		else if(e.getKeyCode() == KeyEvent.VK_UP && dir!=DOWN) dir = UP;
		else if(e.getKeyCode() == KeyEvent.VK_DOWN && dir!=UP) dir = DOWN;
		
		
		//DEBUG
		if(e.getKeyCode() == KeyEvent.VK_U) System.out.println(Arrays.toString(usernames));
		if(e.getKeyCode() == KeyEvent.VK_G) {
			for(int i = 0; i < grid.length; i++) {
				System.out.println(Arrays.toString(grid[i]));
				
			}
		}
		
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
		
	public static void main(String[] args) {
		Random rand = new Random();
		JTextField username_Inp = new JTextField();
		JTextField server_ip_Inp = new JTextField();
		
		//die werte sind nur zum testen
		username_Inp.setText(String.valueOf(rand.nextInt(50000)));
		server_ip_Inp.setText("localhost");

		Object[] message = {
		    "Username:", username_Inp,
		    "Server IP:", server_ip_Inp
		};

		JOptionPane.showConfirmDialog(null, message, "Snake Client", JOptionPane.YES_OPTION);
		ipAdress = server_ip_Inp.getText();
		username = username_Inp.getText();
		
		Game user = new Game(ipAdress);
		
		frame = new JFrame();
		frame.setResizable(false);
		frame.add(user);
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLocationRelativeTo(null); // Frame zentrieren
		frame.setVisible(true);
		frame.setFocusable(true);
		frame.setTitle("SNAKEEEEEE");
		
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		
		
		
		frame.validate();
		user.start();
	}

	
	
	public synchronized void stop() {
		if (!isRunning)
			return;
		isRunning = false;
		try {
			thread.join();
		} catch (InterruptedException e) {
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
	
	public synchronized void start() {
		if (isRunning)
			return;
		
		isRunning = true;
	
		//Spiel in neuem Thread starten
		thread = new Thread(this);
		thread.start();
	}

	public static BufferedImage scale(BufferedImage imageToScale, int dWidth, int dHeight) {
        BufferedImage scaledImage = null;
        if (imageToScale != null) {
            scaledImage = new BufferedImage(dWidth, dHeight, imageToScale.getType());
            Graphics2D graphics2D = scaledImage.createGraphics();
            graphics2D.drawImage(imageToScale, 0, 0, dWidth, dHeight, null);
            graphics2D.dispose();
        }
        return scaledImage;
    }
	
}
