package nicelee.function.music.player.impl.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class WsServer extends WebSocketServer {

	public WsServer(int port) throws UnknownHostException {
		super(new InetSocketAddress(port));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		conn.send("Music Player Server Connected!"); // This method sends a message to the new client
		System.out.println(conn.getRemoteSocketAddress().getAddress().getHostAddress() + " Connected!");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println(conn + " disconnected");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		// Nothing
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer message) {
		// Nothing
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		int port = 8887; // 843 flash policy port
		try {
			port = Integer.parseInt(args[0]);
		} catch (Exception ex) {
		}
		WsServer s = new WsServer(port);
		s.start();

		BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			String in = sysin.readLine();
			s.broadcast(in);
			if (in.equals("exit")) {
				s.stop(1000);
				break;
			}
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
		if (conn != null) {
			// some errors like port binding failed may not be assignable to a specific
			// websocket
		}
	}

	@Override
	public void onStart() {
		System.out.println("Server started!");
		setConnectionLostTimeout(0);
		setConnectionLostTimeout(100);
	}

}
