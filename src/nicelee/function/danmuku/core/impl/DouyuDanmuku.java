package nicelee.function.danmuku.core.impl;

import java.io.IOException;
import java.util.List;

import nicelee.common.annotation.Autoload;
import nicelee.function.danmuku.core.IDanmuku;
import nicelee.function.danmuku.handler.IMsgHandler;
import nicelee.function.danmuku.handler.MsgHandler;

@Autoload(source = "douyu")
public class DouyuDanmuku implements IDanmuku, Runnable {

	List<IMsgHandler> handlers;
	long roomid;
	DouyuSocket douyuSocket;
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
			douyuSocket = new DouyuSocket(roomid, handlers);
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
				System.out.println(roomid + " - douyu发送心跳包成功");
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
