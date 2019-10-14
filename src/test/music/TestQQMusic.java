package test.music;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import nicelee.function.music.source.impl.QQMusic;

public class TestQQMusic {

	@Test
	public void testSearch() {
//		new QQMusic().searchWithoutLink("去年夏天", 2, 1);
		// new QQMusic().source("000wDA7M23CRIf");
		// new QQMusic().source("0035gXZd23PAHQ");
	}

	/**
	 * 搜索歌曲(包含链接)
	 */
	 //@Test
	public void testSearchWithUrl() {
		new QQMusic().searchWithLink("去年夏天", 2, 1);
	}

	/**
	 * 搜索歌曲(包含链接)
	 */
	@Test
	public void testGetLink() {
		QQMusic music = new QQMusic();
		String link = music.source("000wDA7M23CRIf");
		System.out.println(link);
		assertNotNull(link);
		assertEquals(true, link.startsWith("http"));
		
		link = music.source("0035gXZd23PAHQ");
		System.out.println(link);
		assertNull(link);
	}
}
