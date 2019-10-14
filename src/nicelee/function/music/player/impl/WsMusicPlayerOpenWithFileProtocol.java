package nicelee.function.music.player.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.URLEncoder;

import nicelee.common.annotation.Autoload;
import nicelee.function.music.player.IMusicPlayer;
import nicelee.function.music.player.impl.server.WsServer;

@Autoload(source = "ws_fileopen")
public class WsMusicPlayerOpenWithFileProtocol implements IMusicPlayer {

	static WsServer wsServer;
	static Integer WS_PORT;
	static File fplayer;

	static String EXPLORER;
	static {
		try {
			fplayer = new File("AudioPlayer.ws.html");
			if (!fplayer.exists()) {
				BufferedReader buReader = new BufferedReader(new InputStreamReader(
						DefaultMusicPlayerOpenWithFileProtocol.class.getResourceAsStream("/resources/AudioPlayer.ws.html")));
				BufferedWriter buWriter = new BufferedWriter(new FileWriter(fplayer));
				String line = null;
				while ((line = buReader.readLine()) != null) {
					buWriter.write(line);
					buWriter.newLine();
				}
				buReader.close();
				buWriter.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {
		try {
			if (EXPLORER == null)
				EXPLORER = System.getProperty("nicelee.music.player.explorer.path");
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
			// Desktop.getDesktop().open(fplayer.getCanonicalFile());
			Runtime.getRuntime()
					.exec(new String[] { EXPLORER, "file://" + fplayer.getCanonicalPath() + "?port=" + WS_PORT });
		} catch (Exception e) {
			e.printStackTrace();
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
		try {
			wsServer.stop();
		} catch (IOException | InterruptedException e) {
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
