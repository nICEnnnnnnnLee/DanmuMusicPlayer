package nicelee.function.danmuku.core.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.HttpRequestUtil;
import nicelee.common.util.TrustAllCertSSLUtil;
import nicelee.function.danmuku.core.IDanmuku;
import nicelee.function.danmuku.handler.IMsgHandler;
import nicelee.function.danmuku.handler.MsgHandler;

@Autoload(source = "bili")
public class BiliDanmuku implements IDanmuku, Runnable {

	BiliWebsocket webSocket;
	List<IMsgHandler> handlers;
	long roomid;
	Thread hearbeatThread;
	volatile boolean running;

	private BiliDanmuku() {
	}

	private BiliDanmuku(long roomid) {
		this.roomid = roomid;
		running = true;
		hearbeatThread = new Thread(this);
		// 如果接收线程异常退出，心跳线程需要最多一个周期才能反应过来。
		hearbeatThread.setDaemon(true);
		handlers = MsgHandler.getHandlers("bili");
	}

	public static IDanmuku create(long roomid) {
		BiliDanmuku danmuku = new BiliDanmuku(roomid);
		return danmuku;
	}

	@Override
	public boolean start() {
		try {
			HttpRequestUtil util = new HttpRequestUtil();
			// 获取真实roomid
			String roomInfo = util.getContent("https://api.live.bilibili.com/room/v1/Room/room_init?id=" + roomid,
					new HashMap<>());
			long realRoomid = new JSONObject(roomInfo).getJSONObject("data").getLong("room_id");
			// 获取弹幕服务器地址、端口
			String json = util.getContent("https://api.live.bilibili.com/room/v1/Danmu/getConf?room_id=" + realRoomid
					+ "&platform=pc&player=web", new HashMap<>());
			JSONObject obj = new JSONObject(json).getJSONObject("data");
			JSONObject server = obj.getJSONArray("host_server_list").getJSONObject(0);

			String host = server.getString("host");
			int port = server.getInt("wss_port");
			String token = obj.getString("token");

			if (host == null || token == null)
				return false;
			URI url = new URI(String.format("wss://%s:%d/sub", host, port));
			webSocket = new BiliWebsocket(url, roomid, realRoomid, token, handlers);
			webSocket.setSocketFactory(TrustAllCertSSLUtil.getFactory());
			webSocket.connectBlocking();
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
				Thread.sleep(30000);
				webSocket.heartBeat();
			} catch (Exception e) {
				running = false;
			}
		}
		try {
			webSocket.closeBlocking();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(roomid + " - 心跳线程结束");
	}

	@Override
	public List<IMsgHandler> addMsgHandler(IMsgHandler handler) {
		handlers.add(handler);
		return handlers;
	}

}
