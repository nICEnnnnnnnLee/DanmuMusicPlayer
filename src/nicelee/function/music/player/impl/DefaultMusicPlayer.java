package nicelee.function.music.player.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import nicelee.common.annotation.Autoload;
import nicelee.function.music.player.IMusicPlayer;
import nicelee.function.music.player.impl.server.HttpServer;

@Autoload(source = "default", desc = 
"针对chrome对file协议类型无法监听storage变化的问题，转成http需要开一个Socket做简单的服务器")
public class DefaultMusicPlayer implements IMusicPlayer {

	volatile static HttpServer server;
	static Integer PORT;

	@Override
	public void init() {
		if(server == null) {
			try {
				PORT = Integer.parseInt(System.getProperty("nicelee.music.player.http.port"));
			} catch (Exception e) {
			}
			server = new HttpServer(PORT, "AudioPlayer.html");
			PORT = server.open();
		}
		runCommand("action=init");
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
			Thread.sleep(2000);
			server.close();
		} catch (Exception e) {
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
		try {
			Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler http://127.0.0.1:" + PORT + "/?" + cmd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
