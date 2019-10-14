package nicelee.function.music.player;


public interface IMusicPlayer {

	/**
	 * 初始化
	 */
	public void init();
	
	/**
	 * 添加歌曲到播放列表
	 * @param url
	 */
	public void add(String url);
	
	/**
	 * 马上播放歌曲
	 * @param url
	 */
	public void playOnce(String url);
	
	/**
	 * 下一首
	 */
	public void playNext();
	
	/**
	 * 继续播放
	 */
	public void play();
	
	/**
	 * 停止播放
	 */
	public void stop();
	
	/**
	 * 暂停播放
	 */
	public void pause();
	
	/**
	 * 退出
	 */
	public void quit();
}
