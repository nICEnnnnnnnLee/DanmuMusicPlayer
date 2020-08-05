package nicelee.function.danmuku.core.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.HttpRequestUtil;
import nicelee.common.util.TrustAllCertSSLUtil;
import nicelee.function.danmuku.core.IDanmuku;
import nicelee.function.danmuku.handler.IMsgHandler;
import nicelee.function.danmuku.handler.MsgHandler;

@Autoload(source = "douyu")
public class DouyuDanmuku implements IDanmuku, Runnable {

	List<IMsgHandler> handlers;
	long roomid;
	long realRoomId;
	DouyuWebsocket douyuSocket;
	DouyuLoginWebsocket douyuLoginSocket;
	Thread hearbeatThread;
	Thread msgHandlerThread;
	volatile boolean running;

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
			String basicInfoUrl = String.format("https://www.douyu.com/%s", roomid);
			String html = util.getContent(basicInfoUrl, new HashMap<>());
			// System.out.println(html);
			Pattern pRoomId = Pattern.compile("\\$ROOM.room_id ?= ?([0-9]+);");
			Matcher matcher = pRoomId.matcher(html);
			matcher.find();
			realRoomId = Long.parseLong(matcher.group(1));
			
			URI url = new URI("wss://danmuproxy.douyu.com:8501");
			douyuSocket = new DouyuWebsocket(url, roomid, realRoomId, handlers, this);
			douyuSocket.setSocketFactory(TrustAllCertSSLUtil.getFactory());
			douyuSocket.connectBlocking();
			hearbeatThread.start();
			
//			URI urlLogin = new URI("wss://wsproxy.douyu.com:6671");
//			douyuLoginSocket = new DouyuLoginWebsocket(urlLogin, roomid, realRoomId, this);
//			douyuLoginSocket.setSocketFactory(TrustAllCertSSLUtil.getFactory());
//			douyuLoginSocket.connectBlocking();
			
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			running = false;
			return false;
		}
	}

	@Override
	public void stop() {
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
				Thread.sleep(45000);
				douyuSocket.heartBeat();
				//douyuLoginSocket.heartBeat();
				//System.out.println(roomid + " - douyu发送心跳包成功");
			} catch (Exception e) {
				//e.printStackTrace();
				running = false;
			}
		}
		System.out.println(roomid + " - douyu心跳线程结束");
//		try {
//			douyuLoginSocket.closeBlocking();
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
		try {
			douyuSocket.closeBlocking();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public List<IMsgHandler> addMsgHandler(IMsgHandler handler) {
		handlers.add(handler);
		return handlers;
	}

	public DouyuWebsocket getDouyuSocket() {
		return douyuSocket;
	}

	public DouyuLoginWebsocket getDouyuLoginSocket() {
		return douyuLoginSocket;
	}

	public Thread getHearbeatThread() {
		return hearbeatThread;
	}

}
