package nicelee.function.music;

import java.util.HashMap;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.PackageScanLoader;
import nicelee.function.music.domain.Music;
import nicelee.function.music.player.IMusicPlayer;
import nicelee.function.music.source.IMusicAPI;

public class MusicManager {

	static HashMap<String, Class<?>> iMusicSourceMap;
	static HashMap<String, Class<?>> iMusicPlayerMap;
	static {
		iMusicSourceMap = new HashMap<>();
		PackageScanLoader pLoader = new PackageScanLoader() {
			@Override
			public boolean isValid(Class<?> klass) {
				Autoload load = klass.getAnnotation(Autoload.class);
				if (null != load && IMusicAPI.class.isAssignableFrom(klass)) {
					iMusicSourceMap.put(load.source(), klass);
				}
				return false;
			}
		};
		pLoader.scanRoot("nicelee.function.music.source.impl");
		
		iMusicPlayerMap = new HashMap<>();
		pLoader = new PackageScanLoader() {
			@Override
			public boolean isValid(Class<?> klass) {
				Autoload load = klass.getAnnotation(Autoload.class);
				if (null != load && IMusicPlayer.class.isAssignableFrom(klass)) {
					iMusicPlayerMap.put(load.source(), klass);
				}
				return false;
			}
		};
		pLoader.scanRoot("nicelee.function.music.player.impl");
	}
	
	/**
	 * 根据参数获取音乐源实现类
	 * @param source
	 * @return
	 */
	public static IMusicAPI createMusicAPI(String source) {
		Class<?> klass = iMusicSourceMap.get(source);
		try {
			return (IMusicAPI) klass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
	
	
	/**
	 * 根据参数获取播放器
	 * @param type
	 * @return
	 */
	public static IMusicPlayer createMusicPlayer(String type) {
		Class<?> klass = iMusicPlayerMap.get(type);
		try {
			return (IMusicPlayer) klass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			return null;
		}
	}
	
	public static void main(String args[]) throws InterruptedException {

		// System.setProperty("nicelee.music.player.http.port", "7488"); // 不是必需
		//System.setProperty("nicelee.music.player.ws.port", "7488"); // 不是必需 
		System.setProperty("nicelee.music.player.explorer.path", "C:\\Program Files\\Mozilla Firefox\\firefox.exe"); // for file protocal
		IMusicPlayer player = createMusicPlayer("ws_fileopen"); // firefox ws default ws_fileopen
		IMusicAPI api = createMusicAPI("163"); // qq 163
		// 初始化
		player.init();
		// 添加一首歌
		Music music = api.searchWithLink("去年夏天", 1, 1).get(0);
		player.add(music.url);
		// 添加一首歌
		music = api.searchWithLink("战 排骨教主", 1, 1).get(0);
		player.add(music.url);
		// 添加一首歌
		music = api.searchWithLink("GQ", 1, 1).get(0);
		player.add(music.url);

		Thread.sleep(10000);
		// 暂停
		player.pause();
		Thread.sleep(10000);
		// 继续播放
		player.play();
		Thread.sleep(10000);
		// 停止
		player.stop();
		Thread.sleep(10000);
		// 继续播放
		player.play();
		Thread.sleep(10000);
		// 播放下一首
		player.playNext();
		Thread.sleep(5000);
		// 插队播放
		music = api.searchWithLink("Nevada", 1, 1).get(0);
		player.playOnce(music.url);
		Thread.sleep(5000);
		// 退出
		player.quit();
	}
}
