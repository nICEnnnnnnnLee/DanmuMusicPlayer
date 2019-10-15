package nicelee.function.danmuku.core.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.HttpRequestUtil;
import nicelee.function.danmuku.core.IDanmuku;
import nicelee.function.danmuku.handler.IMsgHandler;
import nicelee.function.danmuku.handler.MsgHandler;

@Autoload(source = "douyu")
public class DouyuDanmuku implements IDanmuku, Runnable {

	List<IMsgHandler> handlers;
	long roomid;
	long realRoomid;
	DouyuSocket douyuSocket;
	Thread hearbeatThread;
	Thread msgHandlerThread;
	volatile boolean running;

	final static Pattern realRoomIDPattern = Pattern.compile("\\$ROOM\\.room_id ?=([0-9]+)");
	private DouyuDanmuku() {
	}

	private DouyuDanmuku(long roomid) {
		this.roomid = roomid;
		running = true;
		hearbeatThread = new Thread(this);
		// 如果接收线程异常退出，心跳线程需要最多一个周期才能反应过来。
		hearbeatThread.setDaemon(true); 
		handlers = MsgHandler.getHandlers("douyu");
	}

	
	public static IDanmuku create(long roomid) {
		DouyuDanmuku danmuku = new DouyuDanmuku(roomid);
		return danmuku;
	}

	@Override
	public boolean start() {
		try {
			HttpRequestUtil util = new HttpRequestUtil();
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			headers.put("Accept-Encoding", "gzip, deflate, br");
			headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
			headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0");
			String html = util.getContent("https://www.douyu.com/" + roomid, new HashMap<>());
			//System.out.println(html);
			Matcher matcher = realRoomIDPattern.matcher(html);
			matcher.find();
			realRoomid = Long.parseLong(matcher.group(1));
			douyuSocket = new DouyuSocket(roomid, realRoomid, handlers);
			douyuSocket.loginAndJoinGroup();
			msgHandlerThread = new Thread(douyuSocket);
			msgHandlerThread.start();
			hearbeatThread.start();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			running = false;
			return false;
		}
	}

	@Override
	public void stop() {
		try {
			douyuSocket.logout();
		} catch (IOException e) {
			e.printStackTrace();
		}
		hearbeatThread.interrupt();
	}

	@Override
	public int status() {
		if (running)
			return 1;
		else
			return 0;
	}

	@Override
	public void run() {
		while (running) {
			try {
				douyuSocket.heartBeat();
				Thread.sleep(45000);
				//System.out.println(roomid + " - douyu发送心跳包成功");
			} catch (Exception e) {
				//e.printStackTrace();
				running = false;
			}
		}
		System.out.println(roomid + " - douyu心跳线程结束");
	}
	
	@Override
	public List<IMsgHandler> addMsgHandler(IMsgHandler handler) {
		handlers.add(handler);
		return handlers;
	}

}
