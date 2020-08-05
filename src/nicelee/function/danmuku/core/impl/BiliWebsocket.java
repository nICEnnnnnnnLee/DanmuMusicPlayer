package nicelee.function.danmuku.core.impl;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import nicelee.function.danmuku.domain.Msg;
import nicelee.function.danmuku.domain.User;
import nicelee.function.danmuku.handler.IMsgHandler;

public class BiliWebsocket extends WebSocketClient {
	final int TYPE_LOGIN = 7;
	final int TYPE_HEART_BEAT = 2;

	long roomId;
	long shortId;
	String token;
	List<IMsgHandler> handlers;

	public BiliWebsocket(URI serverUri, long shortId, long roomId, String token, List<IMsgHandler> handlers) {
		super(serverUri);
		this.shortId = shortId;
		this.roomId = roomId;
		this.token = token;
		this.handlers = handlers;
		System.out.println("shortId:" + shortId + " ;roomId:" + roomId);
	}

	/**
	 * 发送登录包
	 */
	public void login() {
		JSONObject obj = new JSONObject();
		obj.put("uid", 0);
		obj.put("roomid", roomId);
		obj.put("protover", 2);
		obj.put("platform", "web");
		obj.put("clientver", "1.10.6");
		obj.put("type", 2);
		obj.put("key", token);
		sendMsg(TYPE_LOGIN, obj.toString());
	}

	/**
	 * 发送心跳包
	 */
	public void heartBeat() {
		String data = "[object Object]";
		sendMsg(TYPE_HEART_BEAT, data);
	}

	/**
	 * 处理消息
	 */
	byte[] bufferRecv = new byte[2048];
	
	private void handleMsg(ByteBuffer blob) {
		int totalSize = blob.getInt() & 0xffffffff;
		blob.position(blob.position() + 12);
//		int headerSize = blob.getShort();
//		int ver = blob.getShort();
//		int oper = blob.getInt();
//		int seq = blob.getInt();
//		System.out.printf("totalSize: %d,headerSize: %d, ver: %d, oper: %d, seq: %d\n", totalSize, headerSize, ver, oper, seq);

		if (totalSize > bufferRecv.length + 16 || totalSize <= 18)
			return;

		blob.get(bufferRecv, 0, totalSize - 16);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Inflater decompressor = new Inflater();
		try {
			decompressor.setInput(bufferRecv, 0, totalSize - 16);
			final byte[] buf = new byte[1024];
			while (!decompressor.finished()) {
				int count = decompressor.inflate(buf);
				bos.write(buf, 0, count);
			}
			byte[] jsonBytes = bos.toByteArray();
			String jsons = new String(jsonBytes, "UTF-8");
			int begin = 0, end = 0;
			begin = jsons.indexOf("{\"cmd\":\"", end);
			while(begin > -1 && end > -1) {
				String json = null;
				end = jsons.indexOf("{\"cmd\":\"", begin + 1);
				if(end > -1) {
					json = jsons.substring(begin, end - 16);
				}else {
					json = jsons.substring(begin);
				}
				begin = end;
				//System.out.println(json);
				JSONObject obj = new JSONObject(json);
				String type = obj.getString("cmd");
				if ("DANMU_MSG".equals(type)) {
					JSONArray array = obj.getJSONArray("info");
					User user = new User();
					user.id = "" + array.getJSONArray(2).optLong(0);
					user.name = array.getJSONArray(2).optString(1);
					long idolNo = array.getJSONArray(3).optLong(3); // 粉丝牌编号
					if (idolNo != roomId)
						user.level = 0;
					else
						user.level = array.getJSONArray(3).optInt(0);
					Msg msg = new Msg();
					msg.type = "DANMU_MSG";
					msg.content = array.optString(1);
					msg.srcUser = user;
					msg.time = array.optJSONArray(0).optLong(4) * 1000;
					for (IMsgHandler handler : handlers) {
						if (!handler.handle(msg, user))
							break;
					}
				} else {
					//System.out.println(json);
				}
			}
			
//			String json = new String(jsonBytes, 16, jsonBytes.length - 16);
//			//System.out.println(json);
		} catch (DataFormatException e) {
			// e.printStackTrace();
		} catch (Exception e) {
			 e.printStackTrace();
		} finally {
			decompressor.end();
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
	byte[] bufferSend = new byte[256];

	private void sendMsg(int oper, String data) {
		byte[] src = data.getBytes();
		int totalSize = 16 + src.length; // 4
		int headerSize = 16; // 2
		int ver = 1; // 2
		// oper 4
		int seq = 1; // 4
		intToByte(totalSize, bufferSend, 0, 4);
		intToByte(headerSize, bufferSend, 4, 2);
		intToByte(ver, bufferSend, 6, 2);
		intToByte(oper, bufferSend, 8, 4);
		intToByte(seq, bufferSend, 12, 4);
		System.arraycopy(src, 0, bufferSend, 16, src.length);
		send(ByteBuffer.wrap(bufferSend, 0, totalSize));
		// printHexString(buffer, totalSize);
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
		System.out.println(shortId + " - webSocket已关闭");
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
		login();
	}

	private static void intToByte(int val, byte[] b, int offset, int len) {
		int i = 0;
		while (i < len) {
			b[offset + len - i - 1] = (byte) ((val >> (8 * i)) & 0xff);
			i++;
		}
	}
}
