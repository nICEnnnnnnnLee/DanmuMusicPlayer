package nicelee.function.music.player.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import nicelee.common.annotation.Autoload;
import nicelee.function.music.player.IMusicPlayer;

@Autoload(source = "firefox", desc = "每一个命令都打开一次文件")
public class DefaultMusicPlayerOpenWithFileProtocol implements IMusicPlayer {

	static String EXPLORER;
	static String COMMAND;

	static {
		try {
			File fplayer = new File("AudioPlayer.html");
			if (!fplayer.exists()) {
				BufferedReader buReader = new BufferedReader(
						new InputStreamReader(DefaultMusicPlayerOpenWithFileProtocol.class.getResourceAsStream("/resources/AudioPlayer.html")));
				BufferedWriter buWriter = new BufferedWriter(new FileWriter(fplayer));
				String line = null;
				while ((line = buReader.readLine()) != null) {
					buWriter.write(line);
				}
				buReader.close();
				buWriter.close();
			}
			COMMAND = "file://" + fplayer.getCanonicalPath() + "?";
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void playNext() {
		runCommand("action=playNext");
	}

	@Override
	public void init() {
		if(EXPLORER == null)
			EXPLORER = System.getProperty("nicelee.music.player.explorer.path");
		runCommand("action=init");
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
		try {
			Runtime.getRuntime().exec(new String[] { EXPLORER, COMMAND + cmd });
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
