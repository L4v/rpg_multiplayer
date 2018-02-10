package a.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import a.main.Game;
import a.main.entities.PlayerMP;
import a.net.packets.Packet;
import a.net.packets.Packet.PacketTypes;
import a.net.packets.Packet00Login;
import a.net.packets.Packet01Disconnect;
import a.net.packets.Packet02Move;

public class GameClient extends Thread {

	private InetAddress ipAdress;
	private DatagramSocket socket;
	private Game game;

	public GameClient(Game game, String ipAdress) {
		this.game = game;
		try {
			this.socket = new DatagramSocket();
			this.ipAdress = InetAddress.getByName(ipAdress);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while (true) {
			byte[] data = new byte[1024];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.parsePacket(packet.getData(), packet.getAddress(), packet.getPort());
			// String message = new String(packet.getData());
			// System.out.println("SERVER > " + message);

		}
	}

	public void sendData(byte[] data) {
		if (!game.isApplet) {
			DatagramPacket packet = new DatagramPacket(data, data.length, ipAdress, 1331);
			try {
				socket.send(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void parsePacket(byte[] data, InetAddress address, int port) {
		String message = new String(data).trim();
		PacketTypes type = Packet.lookupPacket(message.substring(0, 2));
		Packet packet = null;
		switch (type) {
		default:
		case INVALID:
			break;
		case LOGIN:
			packet = new Packet00Login(data);
			handleLogin((Packet00Login) packet, address, port);
			// if (player != null) {
			// this.connectedPlayers.add(player);
			// game.level.addEntity(player);
			// game.player = player;
			// }
			break;
		case DISCONNECT:
			packet = new Packet01Disconnect(data);
			System.out.println("[" + address.getHostAddress() + ":" + port + "] " + ((Packet01Disconnect) packet).getUsername() + " has Left The World.");
			game.level.removePlayerMP(((Packet01Disconnect) packet).getUsername());
			break;
		case MOVE:
			packet = new Packet02Move(data);
			handleMove((Packet02Move) packet);
			break;

		}
	}

	private void handleLogin(Packet00Login packet, InetAddress address, int port) {
		System.out.println("[" + address.getHostAddress() + ":" + port + "] " + (packet).getUsername() + " has joined the game.");
		PlayerMP player = new PlayerMP(game.level, packet.getX(), packet.getY(), (packet).getUsername(), address, port);
		game.level.addEntity(player);
	}

	private void handleMove(Packet02Move packetM) {
		this.game.level.movePlayer(packetM.getUsername(), packetM.getX(), packetM.getY(), packetM.getNumSteps(), packetM.isMoving(), packetM.getMovingDir());
	}
}
