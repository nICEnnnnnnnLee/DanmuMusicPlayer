package nicelee.function.danmuku.core.impl;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import nicelee.function.danmuku.domain.Msg;
import nicelee.function.danmuku.domain.User;
import nicelee.function.danmuku.handler.IMsgHandler;

public class DouyuWebsocket extends WebSocketClient {

	long roomId;
	long realRoomId;

	DouyuDanmuku douyuDanmuku;

	List<IMsgHandler> handlers;
	long tStart;
	long tEnd;

//	static Map<String, String> headers;
//	static {
//		headers = new HashMap<String, String>();
//		headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0");
//		headers.put("Origin", "https://www.douyu.com");
//		headers.put("Host", "danmuproxy.douyu.com:8501");
//		headers.put("Accept", "*/*");
//		headers.put("Accept-Encoding", "gzip, deflate, br");
//		headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
//	}

	public DouyuWebsocket(URI serverUri, long roomId, long realRoomId, List<IMsgHandler> handlers,
			DouyuDanmuku douyuDanmuku) {
//		super(serverUri, headers);
		super(serverUri);
		this.roomId = roomId;
		this.realRoomId = realRoomId;
		this.douyuDanmuku = douyuDanmuku;
		this.handlers = handlers;
	}

	/**
	 * 发送登录包
	 */
	public void login() {
		// /dfl@=sn@AA=105@ASss@AA=1/
		// 发送登录请求
//		String loginCMD = String.format(
//				"type@=loginreq/roomid@=%s/dfl@=sn@AA=105@ASss@AA=1/username@=%s/uid@=%s/ver@=20190610/aver@=218101901/ct@=0/",
//				realRoomId,
//				douyuDanmuku.getDouyuLoginSocket().userName, douyuDanmuku.getDouyuLoginSocket().uid);
		String loginCMD = String.format(
				"type@=loginreq/roomid@=%s/", realRoomId);
//		String loginCMD = String.format("type@=loginreq/roomid@=%s/", realRoomId);
		sendMsg(loginCMD);
	}

	/**
	 * 发送进入分组包
	 */
	public void joinGroup() {
		// 加入弹幕分组开始接收弹幕
		String joinGroupCMD = String.format("type@=joingroup/rid@=%s/gid@=1/", realRoomId);
		sendMsg(joinGroupCMD);
	}

	/**
	 * 发送心跳包
	 */
	public void heartBeat() {
		String heartBeatCMD = String.format("type@=mrkl/");
		//System.out.println("发送心跳包" + heartBeatCMD);
		sendMsg(heartBeatCMD);
//		heartBeatCMD = String.format("type@=keeplive/tick@=%d/", System.currentTimeMillis() / 1000);
//		sendMsg(heartBeatCMD);
	}

	/**
	 * 处理消息
	 */
	final static byte[] bufferRecv = new byte[2048];
	final static Pattern pUID = Pattern.compile("/uid@=([0-9]+)/");
	final static Pattern pLevel = Pattern.compile("/level@=([0-9]+)/");
	final static Pattern pName = Pattern.compile("/nn@=(.*?)/[a-z]+@=");
	final static Pattern pTxt = Pattern.compile("/txt@=(.*?)/[a-z]+@=");

	// bnn@=勋男/bl@=9/brid@=312212
	final static Pattern pFansLevel = Pattern.compile("/bl@=([0-9]+)/");// 粉丝牌等级
	final static Pattern pFansIdol = Pattern.compile("/brid@=([0-9]+)/"); // 带的谁的粉丝牌

	private void handleMsg(ByteBuffer blob) {
		String str;
		synchronized (bufferRecv) {
			// 下条信息的长度
			blob.get(bufferRecv, 0, 4);
			int contentLen = bytesToIntLittle(bufferRecv, 0); // 用小端模式转换byte数组为
			blob.get(bufferRecv, 0, 4);
			blob.get(bufferRecv, 0, 4);
			// int msgType = bytesToIntLittle(buffer, 0);
			contentLen = contentLen - 8;
			blob.get(bufferRecv, 0, contentLen);
			str = new String(bufferRecv, 0, contentLen);
		}
		// System.out.println(str);
		if (str.startsWith("type@=chatmsg")) {
			Matcher m;
			// 用户信息
			User user = new User();
			m = pUID.matcher(str);
			m.find();
			user.id = m.group(1);
			m = pFansIdol.matcher(str);
			m.find();
			if (realRoomId == Long.parseLong(m.group(1))) {
				m = pFansLevel.matcher(str);
				m.find();
				user.level = Integer.parseInt(m.group(1));
			} else {
				user.level = 0;
			}
			m = pName.matcher(str);
			m.find();
			user.name = m.group(1).replace("@S", "/").replace("@A", "@");
			// 弹幕信息
			Msg msg = new Msg();
			msg.type = "chatmsg";
			m = pTxt.matcher(str);
			m.find();
			msg.content = m.group(1).replace("@S", "/").replace("@A", "@");
			msg.srcUser = user;
			msg.time = System.currentTimeMillis();
			for (IMsgHandler handler : handlers) {
				if (!handler.handle(msg, user))
					break;
			}
		} else if (str.startsWith("type@=keeplive")) {
			// System.out.println("收到心跳包" + str);
		} else if (str.startsWith("type@=mrkl")) {
			// System.out.println("收到心跳包" + str);
		} else if (str.startsWith("type@=pingreq")) {
			// System.out.println("收到时间校准包");
			long serverTime = Long.parseLong(str.substring(20, str.length() - 2));
			long clientTime = System.currentTimeMillis();
			long deta = serverTime - clientTime;
			if (deta > 5 * 60 * 1000 || deta < -5 * 60 * 1000) {
				System.err.println("Douyu - 系统时间误差超过5min!");
			}
			joinGroup();
			heartBeat();
		} else if (str.startsWith("type@=loginres")) {
			// System.out.println(str);
		} else if (str.startsWith("type@=noble_num_info")) {
			// heartBeat();
		} else if (str.contains("type@=qausrespond")) {
			// heartBeat();
		}
		if (blob.remaining() > 0) {
			handleMsg(blob);
		}
	}

	/**
	 * 发送数据包
	 * 
	 * @param oper 操作类型
	 * @param data 数据
	 */
	static byte[] bufferSend = new byte[1024];

	private void sendMsg(String data) {
		synchronized (bufferSend) {
			byte[] contents = data.getBytes();
			// 计算消息长度 = 消息长度(4) + 消息类型(2 + 2) + 真实消息内容长度 + 结尾标识长度(1)
			int contenLeng = 4 + 4 + contents.length + 1;

			intToBytesLittle(contenLeng, bufferSend, 0);
			intToBytesLittle(contenLeng, bufferSend, 4);
			intToBytesLittle(689, bufferSend, 8);// 文本格式类型（加密0 + 保留0）
			System.arraycopy(contents, 0, bufferSend, 12, contents.length);
			bufferSend[12 + contents.length] = 0; // 标识数据结尾
			// send(ByteBuffer.wrap(bufferSend, 0, 13 + contents.length));
			// printHexString(bufferSend, 13 + contents.length);

			ByteArrayOutputStream out = new ByteArrayOutputStream(13 + contents.length);
			out.write(bufferSend, 0, 13 + contents.length);
			send(out.toByteArray());
			// printHexString(out.toByteArray(), out.toByteArray().length);
		}
	}

	/**
	 * 以小端模式将int转成byte[]
	 *
	 * @param value
	 * @return
	 */
	private static void intToBytesLittle(int value, byte[] src, int offset) {
		src[offset + 3] = (byte) ((value >> 24) & 0xFF);
		src[offset + 2] = (byte) ((value >> 16) & 0xFF);
		src[offset + 1] = (byte) ((value >> 8) & 0xFF);
		src[offset] = (byte) (value & 0xFF);
	}

	/**
	 * 以小端模式将byte[]转成int
	 */
	private static int bytesToIntLittle(byte[] src, int offset) {
		int value;
		value = (int) ((src[offset] & 0xFF) | ((src[offset + 1] & 0xFF) << 8) | ((src[offset + 2] & 0xFF) << 16)
				| ((src[offset + 3] & 0xFF) << 24));
		return value;
	}

	/**
	 * 将指定byte数组以16进制的形式打印到控制台
	 *
	 * @param hint String
	 * @param b    byte[]
	 * @return void
	 */
	public static void printHexString(byte[] b, int length) {
		for (int i = 0; i < length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			System.out.print("0x" + hex.toUpperCase());
			if (i != length - 1)
				System.out.print(", ");
		}
		System.out.println("");
	}

	@Override
	public void onClose(int arg0, String arg1, boolean arg2) {
		System.out.println(roomId + " - webSocket已关闭");
		tEnd = System.currentTimeMillis();
		System.out.printf("连接了 %d s\r\n", (tEnd - tStart) / 1000);
	}

	@Override
	public void onError(Exception arg0) {

	}

	@Override
	public void onMessage(String arg0) {
		System.out.println(arg0);
	}

	@Override
	public void onMessage(ByteBuffer blob) {
		handleMsg(blob);
	}

	@Override
	public void onOpen(ServerHandshake arg0) {
		System.out.println("已连接，尝试登录房间");
		tStart = System.currentTimeMillis();
		login();
//		joinGroup();
//		heartBeat();
	}
}
