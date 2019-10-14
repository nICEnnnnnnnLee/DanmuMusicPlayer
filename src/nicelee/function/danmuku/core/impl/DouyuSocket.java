package nicelee.function.danmuku.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nicelee.common.annotation.Autoload;
import nicelee.function.danmuku.domain.Msg;
import nicelee.function.danmuku.domain.User;
import nicelee.function.danmuku.handler.IMsgHandler;

@Autoload(source = "douyu")
public class DouyuSocket implements Runnable {

	long roomId;
	List<IMsgHandler> handlers;
	Socket socket;
	InputStream in;
	OutputStream out;
	byte[] bufferRecv = new byte[1024*10];
	byte[] bufferSend = new byte[2048];

	public DouyuSocket(long roomId, List<IMsgHandler> handlers) {
		this.roomId = roomId;
		this.handlers = handlers;
	}

	/**
	 * 登录并进组
	 * 
	 * @return
	 */
	public boolean loginAndJoinGroup() {
		try {
			socket = new Socket();
			InetSocketAddress addr = new InetSocketAddress("openbarrage.douyutv.com", 8601);
			socket.connect(addr, 10000);
			// TODO 是真实id，还是url id
			//socket.setTcpNoDelay(true);
			//socket.setKeepAlive(on);
			in = socket.getInputStream();
			out = socket.getOutputStream();
			// 发送登录请求(登入9999房间)
			String loginCMD = String.format("type@=loginreq/roomid@=%s/", roomId);
			sendSingleMsg(loginCMD);

			// 读取登录请求消息
			readSingleMsg();
			// int msgLenth = readSingleMsg();
			// String msg = new String(bufferRecv, 0, msgLenth);
			// System.out.println(msg);
			// 加入弹幕分组开始接收弹幕
			String joinGroupCMD = String.format("type@=joingroup/rid@=%s/gid@=-9999/", roomId);
			sendSingleMsg(joinGroupCMD);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 发送心跳包
	 * 
	 * @throws IOException
	 */
	public void heartBeat() throws IOException {
		String heartBeatCMD = String.format("type@=keeplive/tick@=%d/", System.currentTimeMillis() / 1000);
		System.out.println("发送心跳包" + heartBeatCMD);
		sendSingleMsg(heartBeatCMD);
	}

	/**
	 * 登出
	 * 
	 * @throws IOException
	 */
	public void logout() throws IOException {
		// 发送登录请求(登入9999房间)
		String logoutCMD = String.format("type@=logout/");
		sendSingleMsg(logoutCMD);
		socket.close();
	}

	/**
	 * 发送消息
	 *
	 * @param content
	 * @throws IOException
	 */
	private void sendSingleMsg(String content) throws IOException {
		synchronized (this) {
			// 计算消息长度 = 消息长度(4) + 消息类型(2 + 2) + 真实消息内容长度 + 结尾标识长度(1)
			int contenLeng = 4 + 4 + content.length() + 1;

			intToBytesLittle(contenLeng, bufferSend, 0);
			intToBytesLittle(contenLeng, bufferSend, 4);
			intToBytesLittle(689, bufferSend, 8);// 文本格式类型（加密0 + 保留0）
			byte[] contents = content.getBytes("UTF-8");
			System.arraycopy(contents, 0, bufferSend, 12, contents.length);
			bufferSend[12 + contents.length] = 0; // 标识数据结尾

			// 发送数据
			out.write(bufferSend, 0, 13 + contents.length);
			out.flush();
		}
	}

	/**
	 * 读取消息
	 *
	 * @return 消息长度
	 * @throws IOException
	 */
	private String readSingleMsg() throws IOException {
		int contentLen = 0, readLen= 0;
		try {
			synchronized (this) {
				// 下条信息的长度
				in.read(bufferRecv, 0, 4);
				contentLen = bytesToIntLittle(bufferRecv, 0); // 用小端模式转换byte数组为
				in.read(bufferRecv, 0, 4);
//				int contentLen2 = bytesToIntLittle(buffer, 0);
				in.read(bufferRecv, 0, 4);
//				int msgType = bytesToIntLittle(buffer, 0);
				//
				contentLen = contentLen - 8;
				// 继续读取真正的消息内容
				readLen = 0;
				int len = in.read(bufferRecv, 0, contentLen); // 本次读取数据长度
				//len = len == -1? 0:len;
				readLen += len; // 已读数据长度
				while (len != -1 && readLen < contentLen) {
					len = in.read(bufferRecv, readLen, contentLen - readLen);
//					if(len == -1) {
//						len = len == -1? 0:len;
//						System.out.println();
//					}
					readLen += len;
				}
				return new String(bufferRecv, 0, readLen);
			}
		}catch (Exception e) {
			System.out.println("消息长度：" + contentLen);
			System.out.println("读取长度：" + readLen);
			throw e;
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

	final static Pattern pUID = Pattern.compile("/uid@=([0-9]+)/");
	final static Pattern pLevel = Pattern.compile("/level@=([0-9]+)/");
	final static Pattern pName = Pattern.compile("/nn@=(.*?)/[a-z]+@=");
	final static Pattern pTxt = Pattern.compile("/txt@=(.*?)/[a-z]+@=");
	
	//bnn@=勋男/bl@=9/brid@=312212
	final static Pattern pFansLevel = Pattern.compile("/bl@=([0-9]+)/");// 粉丝牌等级
	final static Pattern pFansIdol = Pattern.compile("/brid@=([0-9]+)/"); // 带的谁的粉丝牌

	@Override
	public void run() {
		try {
			while (true) {
				String str = readSingleMsg();
				//System.out.println(str);

				if (str.startsWith("type@=chatmsg")) {
					Matcher m;
					// 用户信息
					User user = new User();
					m = pUID.matcher(str);
					m.find();
					user.id = m.group(1);
					m = pFansIdol.matcher(str);
					m.find();
					if(roomId == Long.parseLong(m.group(1))) {
						m = pFansLevel.matcher(str);
						m.find();
						user.level = Integer.parseInt(m.group(1));
					}else {
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
					System.out.println("收到心跳包" + str);
				} else if (str.startsWith("type@=pingreq")) {
					System.out.println("时间校准包");
					long serverTime = Long.parseLong(str.substring(20, str.length() -2));
					long clientTime = System.currentTimeMillis();
					long deta = serverTime - clientTime;
					if(deta > 5*60*1000 || deta < -5*60*1000) {
						System.err.println("系统时间误差超过5min!");
						throw new Exception("Douyu - 系统时间误差超过5min!");
					}
				}
				Thread.sleep(1);
			}

		} catch (Exception e) {
			 e.printStackTrace();
			try {
				socket.close();
			} catch (IOException e1) {
			}
		}
		System.out.println(roomId + " - Douyu弹幕接收进程结束");
	}

}
