package nicelee.function.danmuku.handler;

import nicelee.function.danmuku.domain.Msg;
import nicelee.function.danmuku.domain.User;

public interface IMsgHandler {

	public boolean handle(Msg msg, User user);
}
