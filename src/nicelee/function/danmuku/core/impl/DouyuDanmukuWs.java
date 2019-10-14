package nicelee.function.danmuku.core.impl;

import java.net.URI;
import java.util.List;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.TrustAllCertSSLUtil;
import nicelee.function.danmuku.core.IDanmuku;
import nicelee.function.danmuku.handler.IMsgHandler;
import nicelee.function.danmuku.handler.MsgHandler;

@Autoload(source = "douyuWs")
public class DouyuDanmukuWs implements IDanmuku, Runnable {

	List<IMsgHandler> handlers;
	long roomid;
	DouyuWebsocket douyuSocket;
	Thread hearbeatThread;
	Thread msgHandlerThread;
	volatile boolean running;

	private DouyuDanmukuWs() {
	}

	private DouyuDanmukuWs(long roomid) {
		this.roomid = roomid;
		running = true;
		hearbeatThread = new Thread(this);
		// 如果接收线程异常退出，心跳线程需要最多一个周期才能反应过来。
		hearbeatThread.setDaemon(true); 
		handlers = MsgHandler.getHandlers("douyu");
	}

	public static IDanmuku create(long roomid) {
		DouyuDanmukuWs danmuku = new DouyuDanmukuWs(roomid);
		return danmuku;
	}

	@Override
	public boolean start() {
		try {
			URI url = new URI("wss://danmuproxy.douyu.com:8504");
			douyuSocket = new DouyuWebsocket(url, roomid, handlers);
			douyuSocket.setSocketFactory(TrustAllCertSSLUtil.getFactory());
			douyuSocket.connectBlocking();
			
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
				Thread.sleep(10000);
				douyuSocket.heartBeat();
				System.out.println(roomid + " - douyu发送心跳包成功");
			} catch (Exception e) {
				running = false;
			}
			try {
				douyuSocket.closeBlocking();
			}catch (Exception e) {
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
