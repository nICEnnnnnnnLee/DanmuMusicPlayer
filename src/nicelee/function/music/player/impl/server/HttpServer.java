package nicelee.function.music.player.impl.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import nicelee.function.music.player.impl.DefaultMusicPlayer;

public class HttpServer extends Thread {

	volatile static String HTML;

	ServerSocket server;
	Integer PORT;
	Boolean running;

	public HttpServer(Integer port, String file) {
		this.setDaemon(true);
		PORT = port;
		if(HTML == null) {
			try {
				StringBuilder sb = new StringBuilder();
				BufferedReader buReader = new BufferedReader(
						new InputStreamReader(DefaultMusicPlayer.class.getResourceAsStream("/resources/" + file)));
				String line = null;

				while ((line = buReader.readLine()) != null) {
					sb.append(line).append("\r\n");
				}
				HTML = sb.toString();
				buReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public int open() {
		try {
			if (PORT == null) {
				server = new ServerSocket(0);
				PORT = server.getLocalPort();
			} else {
				server = new ServerSocket(PORT);
				System.out.println("监听了端口：" + PORT);
			}
			// this.setDaemon(true);
			running = true;
			this.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return PORT;
	}

	public void close() {
		try {
			running = false;
			server.close();
		} catch (IOException e) {
		}
	}

	@Override
	public void run() {
		try {
			while (running) {
				BufferedReader in = null;
				BufferedWriter out = null;
				try {
					Socket socket = server.accept();
					in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
					out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
					// 读取url请求
					String line = null;
					while ((line = in.readLine()) != null) {
						// 处理结尾
						if (line.length() == 0) {
							break;
						}
					}
					// 返回结果
					out.write("HTTP/1.1 200 OK\r\n");
					out.write("Content-Type: text/html; charset=UTF-8\r\n");
//					out.write("Content-Length: "+ html.length()+ "\r\n");
					out.write("\r\n");
					// 处理请求并返回内容
					out.write(HTML);
					out.write("\r\n");
					out.flush();
				} catch (SocketTimeoutException e) {
					continue;
				} catch (SocketException e) {
					break;
				} finally {
					try {
						in.close();
					} catch (IOException e) {
					}
					try {
						out.close();
					} catch (IOException e) {
					}
				}
			}
		} catch (Exception e) {
		} finally {
			try {
				server.close();
			} catch (IOException e) {
			}
			running = false;
		}

	}
}
