package nicelee.function.danmuku.handler.impl;


import nicelee.common.annotation.Autoload;
import nicelee.function.danmuku.domain.Msg;
import nicelee.function.danmuku.domain.User;
import nicelee.function.danmuku.handler.IMsgHandler;

@Autoload(source = "bili")
public class BiliMsgHandler implements IMsgHandler{

	/**
	 * 返回true代表继续执行该任务链
	 */
	@Override
	public boolean handle(Msg msg, User user) {
		System.out.printf("%d %s(%s) 粉丝等级%d: %s\n", msg.time, user.name, user.id, user.level, msg.content);
		return true;
	}

}
