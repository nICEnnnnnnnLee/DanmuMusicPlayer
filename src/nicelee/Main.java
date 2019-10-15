package nicelee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nicelee.function.danmuku.DanmukuManager;
import nicelee.function.danmuku.core.IDanmuku;
import nicelee.function.danmuku.domain.Msg;
import nicelee.function.danmuku.domain.User;
import nicelee.function.danmuku.handler.IMsgHandler;
import nicelee.function.music.MusicManager;
import nicelee.function.music.domain.Music;
import nicelee.function.music.player.IMusicPlayer;
import nicelee.function.music.source.IMusicAPI;

public class Main {

	public static void main(String[] args) {
//		args = new String[] {"douyu", "233233","163",  "0"};
//		args = new String[] {"bili", "6","163",  "0"};
		
		String danmuSource = "bili";// Support bili. Plan to Support douyu
		String musicSource = "163";// Support qq 163
		long roomId = 0;
		int level = 0;
		try {
			danmuSource = args[0];
			roomId = Long.parseLong(args[1]);
			musicSource = args[2];
			level = Integer.parseInt(args[3]);
		} catch (Exception e) {
			System.err.println("参数传入错误！！");
			e.printStackTrace();
		}
		final int userAllowToDoso = level;
//		System.setProperty("nicelee.music.player.explorer.path", "C:\\Program Files\\Mozilla Firefox\\firefox.exe"); 
		System.out.println("输入q 或 stop退出");
		// 各种初始化
		IDanmuku danmu = DanmukuManager.createDanmuku(danmuSource, roomId);
		IMusicAPI api = MusicManager.createMusicAPI(musicSource);
		IMusicPlayer player = MusicManager.createMusicPlayer("ws");
		player.init();

		// 弹幕处理逻辑
		Pattern pattern = Pattern.compile("^点歌 ?(.*)");
		danmu.addMsgHandler(new IMsgHandler() {
			@Override
			public boolean handle(Msg msg, User user) {
				// 粉丝牌大于等于userAllowToDoso级别，开启点歌功能
				if (user.level >= userAllowToDoso) {
					Matcher matcher = pattern.matcher(msg.content);
					if (matcher.find()) {
						List<Music> result = api.searchSimilar(matcher.group(1), 5, 1);
						if (result.size() > 0) {
							System.out.println("找到最匹配歌曲,现在添加到播放列表");
							player.add(result.get(0).url);
						}
					}
				}
				return true;
			}
		});
		danmu.start();

		try {
			BufferedReader reader;
			reader = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
			String command = reader.readLine();
			while (!command.startsWith("q") && !command.startsWith("stop")) {
				System.out.println("输入q 或 stop退出");
				command = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// 各种结束
		danmu.stop();
		player.quit();
		System.out.println("结束");

	}

}
