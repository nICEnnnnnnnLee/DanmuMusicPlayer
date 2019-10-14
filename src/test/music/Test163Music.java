package test.music;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


import org.json.JSONObject;
import org.junit.Test;

import nicelee.function.music.source.impl.NetEaseMusic;
import nicelee.function.music.source.impl.NetEaseEncryptUtil;

public class Test163Music {

	public Test163Music() {
//		System.setProperty("proxyHost", "127.0.0.1");//
//		System.setProperty("proxyPort", "8888");//
//		try {
//			HttpsURLConnection.setDefaultSSLSocketFactory(TrustAllCertSSLUtil.getFactory());
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
	}

	// @Test
	public void testEncrypt() {
		JSONObject obj = new JSONObject();
		obj.put("s", "summer");
		obj.put("csrf_token", "");
		String param1 = obj.toString();
		try {
			String param = NetEaseEncryptUtil.generateToken(param1);
			System.out.println(obj.toString());
			System.out.println(param);
			param = NetEaseEncryptUtil.generateToken(param1);
			System.out.println(obj.toString());
			System.out.println(param);
			param = NetEaseEncryptUtil.generateToken(param1);
			System.out.println(obj.toString());
			System.out.println(param);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// @Test
	public void testSearch() throws Exception {
		new NetEaseMusic().searchWithoutLink("去年夏天 王大毛", 5, 1);
	}

	/**
	 * 搜索歌曲(包含链接)
	 */
	@Test
	public void testSearchWithUrl() {
		new NetEaseMusic().searchWithLink("去年夏天", 2, 1);
	}

	/**
	 * 搜索歌曲(包含链接)
	 */
	@Test
	public void testGetLink() {
		NetEaseMusic music = new NetEaseMusic();
		String link = music.source("557581476");
		System.out.println(link);
		assertNotNull(link);
		assertEquals(true, link.startsWith("http"));

		link = music.source("1234567");
		System.out.println(link);
		assertNull(link);
	}
}
