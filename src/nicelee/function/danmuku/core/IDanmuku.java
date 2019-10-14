package nicelee.function.danmuku.core;

import java.util.List;

import nicelee.function.danmuku.handler.IMsgHandler;

public interface IDanmuku {

	public boolean start();
	
	public void stop();
	
	public int status();
	
	public List<IMsgHandler> addMsgHandler(IMsgHandler handler);
}
