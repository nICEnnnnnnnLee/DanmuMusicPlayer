package nicelee.function.danmuku.core.impl;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import nicelee.common.util.MD5;


public class DouyuLoginWebsocket extends WebSocketClient {

	long roomId;
	long realRoomId;
	
	DouyuDanmuku douyuDanmuku;
	
	public String userName;
	public String uid;
//	static Map<String, String> headers;
//	static {
//		headers = new HashMap<String, String>();
//		headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0");
//		headers.put("Origin", "https://www.douyu.com");
////		headers.put("Accept", "*/*");
////		headers.put("Accept-Encoding", "gzip, deflate, br");
////		headers.put("Accept-Language", "zh-CN,zh;q=0.8,zh-TW;q=0.7,zh-HK;q=0.5,en-US;q=0.3,en;q=0.2");
//	}
	
	public DouyuLoginWebsocket(URI serverUri, long roomId, long realRoomId, DouyuDanmuku douyuDanmuku) {
//		super(serverUri, headers);
		super(serverUri);
		this.roomId = roomId;
		this.realRoomId = realRoomId;
		this.douyuDanmuku = douyuDanmuku;
	}

	/**
	 * 发送游客登录包
	 */
	public void login() {
		String devid = getRandom(32);//"246de9a2c514366209f1665100041501";
		long rt = System.currentTimeMillis() / 1000;
		String vk = MD5.encrypt(rt +"r5*^5;}2#${XF[h+;'./.Q'1;,-]f'p[" + devid);
		// 发送登录请求
		String loginCMD = String.format(
				"type@=loginreq/roomid@=%s/dfl@=sn@AA=105@ASss@AA=1/username@=/password@=/ltkid@=/biz@=/stk@=/devid@=%s/ct@=0/pt@=2/cvr@=0/tvr@=7/apd@=/rt@=%d/vk@=%s/ver@=20190610/aver@=218101901/dmbt@=firefox/dmbv@=79/",
				realRoomId, devid, rt, vk);
		System.out.println(loginCMD);
		sendMsg(loginCMD);
	}

	/**
	 * 发送心跳包
	 */
	public void heartBeat() {
//		String h5ckreq = String.format("type@=h5ckreq/rid@=%s/ti@=220120200804/", realRoomId);
//		sendMsg(h5ckreq);
		String heartBeatCMD = String.format("type@=keeplive/vbw@=0/cdn@=/tick@=%d/kd@=/", System.currentTimeMillis()/1000);
		// System.out.println("发送心跳包" + heartBeatCMD);
		sendMsg(heartBeatCMD);
	}

	/**
	 * 处理消息
	 */
	final static byte[] bufferRecv = new byte[2048];
	final static Pattern pUID = Pattern.compile("/userid@=([0-9]+)/");
	final static Pattern pUserName = Pattern.compile("/username@=([^/]+)/");

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
		//System.out.println(str);
		if (str.startsWith("type@=loginres")) {
			System.out.println(str);
			Matcher m = pUID.matcher(str);
			m.find();
			uid = m.group(1);
			m = pUserName.matcher(str);
			m.find();
			userName = m.group(1);
			System.out.printf("userName: %s, uid: %s \r\n", userName, uid);
		} else if (str.startsWith("type@=ittl")) {
			try {
				douyuDanmuku.getDouyuSocket().connectBlocking();
				douyuDanmuku.getHearbeatThread().start();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if(str.startsWith("type@=h5ckres")){
			System.out.println(str);
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
			//send(ByteBuffer.wrap(bufferSend, 0, 13 + contents.length));
			// printHexString(bufferSend, 13 + contents.length); 

			ByteArrayOutputStream out = new ByteArrayOutputStream(13 + contents.length);
			out.write(bufferSend, 0, 13 + contents.length);
			send(out.toByteArray());
			//printHexString(out.toByteArray(), out.toByteArray().length);

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
		System.out.println(roomId + "- login - webSocket已关闭");
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
		login();
		// joinGroup();
		// heartBeat();
	}
	
	private static String getRandom(int i) {// 随机16字符即可
		StringBuilder sb = new StringBuilder(i);
		String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
		for (int j = 0; j < i; j++) {
			int m = (int) (Math.random() * alphabet.length());
			sb.append(alphabet.charAt(m));
		}
		return sb.toString();
	}
}
