package a.main;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import a.gfx.Screen;
import a.gfx.SpriteSheet;
import a.level.Level;
import a.main.entities.Player;
import a.main.entities.PlayerMP;
import a.net.GameClient;
import a.net.GameServer;
import a.net.packets.Packet00Login;

public class Game extends Canvas implements Runnable {

	private static final long serialVersionUID = 1L;

	public boolean server = false;

	public static final int WIDTH = 160, HEIGHT = WIDTH / 12 * 9;
	public static final int SCALE = 3;
	public static final String name = "Game";
	public static final Dimension DIMENSIONS = new Dimension(WIDTH * SCALE, HEIGHT * SCALE);
	public static Game game;
	private Thread thread;

	public JFrame frame;

	public boolean running = false;
	public int tickCount = 0;

	private BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	private int[] pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
	private int[] colours = new int[6 * 6 * 6];

	private Screen screen;
	public InputHandler input;
	public Level level;
	public Player player;
	public WindowHandler windowhandler;

	public GameClient socketClient;
	public GameServer socketServer;

	public boolean debug = true;
	public boolean isApplet = false;

	public Game() {
		setMinimumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		setMaximumSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
		setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));

		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		frame.setTitle(name);
		frame.add(this, BorderLayout.CENTER);
		frame.pack();

		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public void init() {
		game = this;
		int index = 0;
		for (int r = 0; r < 6; r++) {
			for (int g = 0; g < 6; g++) {
				for (int b = 0; b < 6; b++) {
					int rr = (r * 255 / 5);
					int gg = (g * 255 / 5);
					int bb = (b * 255 / 5);

					colours[index++] = rr << 16 | gg << 8 | bb;
				}
			}
		}

		screen = new Screen(WIDTH, HEIGHT, new SpriteSheet("/sprite_sheet.png"));
		input = new InputHandler(this);
		level = new Level("/levels/water_level.png");
		player = new PlayerMP(level, 100, 100, input, JOptionPane.showInputDialog(this, "Please enter a username."), null, -1);
		level.addEntity(player);
		if (!isApplet) {
			Packet00Login loginPacket = new Packet00Login(player.getUsername(), player.x, player.y);
			if (socketServer != null) {
				socketServer.addConnection((PlayerMP) player, loginPacket);
			}
			// socketClient.sendData("ping".getBytes());
			loginPacket.writeData(socketClient);
		}
	}

	public static void main(String[] args) {
		new Game().start();
	}

	public synchronized void start() {
		running = true;
		thread = new Thread(this, name + "_main");
		thread.start();
		if (!isApplet) {
			if (JOptionPane.showConfirmDialog(this, "Do you want to run on a server", null, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				socketServer = new GameServer(this);
				socketServer.start();
			}
			socketClient = new GameClient(this, "localhost");
			socketClient.start();
		}
	}

	public synchronized void stop() {
		running = false;

		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		long lastTime = System.nanoTime();
		double nsPerTick = 1000000000D / 60D;

		int ticks = 0;
		int frames = 0;

		long lastTimer = System.currentTimeMillis();
		double delta = 0;

		init();

		while (running) {
			long now = System.nanoTime();
			delta += (now - lastTime) / nsPerTick;
			lastTime = now;
			boolean shouldRender = true;
			while (delta >= 1) {
				ticks++;
				tick();
				delta -= 1;
				shouldRender = true;
			}
			try {
				Thread.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (shouldRender) {
				frames++;
				render();
			}
			if (System.currentTimeMillis() - lastTimer > 1000) {
				lastTimer += 1000;
				debug(DebugLevel.INFO, frames + ":frames" + ", " + ticks + ":ticks");
				frames = 0;
				ticks = 0;
			}
		}
	}

	public void tick() {
		tickCount++;

		level.tick();
	}

	public void render() {
		BufferStrategy bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(3);
			return;
		}

		int xOffset = player.x - (screen.width / 2);
		int yOffset = player.y - (screen.height / 2);

		level.renderTiles(screen, xOffset, yOffset);
		level.renderEntities(screen);

		for (int y = 0; y < screen.height; y++) {
			for (int x = 0; x < screen.width; x++) {
				int colourCode = screen.pixels[x + y * screen.width];
				if (colourCode < 255) pixels[x + y * WIDTH] = colours[colourCode];
			}
		}

		Graphics2D g = (Graphics2D) bs.getDrawGraphics();
		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);

		g.dispose();
		bs.show();

		// Graphics2D g2 = (Graphics2D) lightmap.getGraphics();
		// g2.setColor(new Color(0, 0, 0, 255));
		// g2.fillRect(0, 0, lightmap.getWidth(), lightmap.getHeight());
		// g2.setComposite(AlphaComposite.DstOut);

	}

	public void debug(DebugLevel level, String message) {
		switch (level) {
		default:
		case INFO:
			if (debug) {
				System.out.println("[" + name + "] " + message);
			}
			break;
		case WARNING:
			System.out.println("[" + name + "]" + "[WARNING] " + message);

			break;
		case SEVERE:
			System.out.println("[" + name + "]" + "[SEVERE] " + message);
			this.stop();
			break;

		}
	}

	public static enum DebugLevel {
		INFO, WARNING, SEVERE;
	}
}
