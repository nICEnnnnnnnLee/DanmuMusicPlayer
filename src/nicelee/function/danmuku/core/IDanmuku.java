package nicelee.function.danmuku.core;

import java.util.List;

import nicelee.function.danmuku.handler.IMsgHandler;

public interface IDanmuku {
	
	// 请实现静态实例化create方法
	//public static IDanmuku create(long roomId) 
	
	public boolean start();
	
	public void stop();
	
	public int status();
	
	public List<IMsgHandler> addMsgHandler(IMsgHandler handler);
}
