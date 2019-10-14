package nicelee.function.music.player.impl;

import java.awt.Desktop;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;

import nicelee.common.annotation.Autoload;
import nicelee.function.music.player.IMusicPlayer;
import nicelee.function.music.player.impl.server.HttpServer;
import nicelee.function.music.player.impl.server.WsServer;

@Autoload(source = "ws")
public class WsMusicPlayer implements IMusicPlayer {

	volatile static WsServer wsServer;
	static Integer WS_PORT;
	volatile static HttpServer httpServer;
	static Integer HTTP_PORT;

	@Override
	public void init() {
		try {
			if (wsServer == null) {
				try {
					WS_PORT = Integer.parseInt(System.getProperty("nicelee.music.player.ws.port"));
				} catch (Exception e) {
					ServerSocket so = new ServerSocket(0);
					so.close();
					WS_PORT = so.getLocalPort();
				}
				wsServer = new WsServer(WS_PORT);
				wsServer.start();
			}
			if (httpServer == null) {
				try {
					HTTP_PORT = Integer.parseInt(System.getProperty("nicelee.music.player.http.port"));
				} catch (Exception e) {
				}
				httpServer = new HttpServer(HTTP_PORT, "AudioPlayer.ws.html");
				HTTP_PORT = httpServer.open();
			}
			Desktop.getDesktop().browse(new URI("http://127.0.0.1:" + HTTP_PORT + "/?port=" + WS_PORT));
//			Runtime.getRuntime()
//					.exec("rundll32 url.dll,FileProtocolHandler http://127.0.0.1:" + HTTP_PORT + "/?port=" + WS_PORT);
		} catch (Exception e) {
		}
	}

	@Override
	public void playNext() {
		runCommand("action=playNext");
	}

	@Override
	public void play() {
		runCommand("action=continue");
	}

	@Override
	public void stop() {
		runCommand("action=stop");
	}

	@Override
	public void pause() {
		runCommand("action=pause");
	}

	@Override
	public void quit() {
		runCommand("action=quit");
		httpServer.close();
		try {
			wsServer.stop();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void add(String url) {
		try {
			runCommand("action=play&param=" + URLEncoder.encode(url, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void playOnce(String url) {
		try {
			runCommand("action=playOnce&param=" + URLEncoder.encode(url, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void runCommand(String cmd) {
		wsServer.broadcast(cmd);
	}

}
