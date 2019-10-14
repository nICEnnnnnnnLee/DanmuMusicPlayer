package nicelee.function.music.source.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import org.json.JSONArray;
import org.json.JSONObject;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.HttpRequestUtil;
import nicelee.function.music.domain.Music;
import nicelee.function.music.source.IMusicAPI;

@Autoload(source = "163")
public class NetEaseMusic implements IMusicAPI {

	HttpRequestUtil util;

	public NetEaseMusic() {
		util = new HttpRequestUtil();
	}

	@Override
	public List<Music> searchWithoutLink(String keyWord, int pageSize, int pn) {
		List<Music> list = new ArrayList<Music>();
		try {
			String url = "https://music.163.com/weapi/cloudsearch/get/web?csrf_token=";
			HashMap<String, String> headers = new HashMap<>();
			headers.put("content-type", "application/x-www-form-urlencoded");
			headers.put("referer", "https://music.163.com/search/");
			headers.put("accept", "*/*");
			headers.put("origin", "https://music.163.com");
			headers.put("accept-encoding", "gzip, deflate, sdch, br");
			headers.put("accept-language", "zh-CN,zh;q=0.8");
			headers.put("user-agent",
					"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 SE 2.X MetaSr 1.0");
			
			JSONObject obj = new JSONObject();
			obj.put("s", keyWord);
			obj.put("offset", pn -1);
			obj.put("limit", pageSize);
			obj.put("type", "1"); //type 单曲1，歌手100，专辑10，MV1004，歌词1006，歌单1000，主播电台1009，用户1002
			obj.put("csrf_token", "");
//			obj.put("total", "true");
			String params = NetEaseEncryptUtil.generateToken(obj.toString());
			String result = util.postContent(url, headers, params);
//			System.out.println(result);
			
			JSONArray jsonArr = new JSONObject(result).getJSONObject("result").getJSONArray("songs");
			for (int i = 0; i < jsonArr.length(); i++) {
				// System.out.printf(" 当前第一个歌曲%d信息:\n",i+1);
				Music song = new Music();
				JSONObject json = jsonArr.getJSONObject(i);
				song.id = "" + json.getLong("id");
				song.name = json.getString("name");
				// 有多个singer时，默认只取首个
				song.singer = json.getJSONArray("ar").getJSONObject(0).getString("name");
				song.album = json.getJSONObject("al").getString("name");
				// song.print();
				list.add(song);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	@Override
	public List<Music> searchWithLink(String keyWord, int pageSize, int pn) {
		List<Music> list = searchWithoutLink(keyWord, pageSize, pn);
		for (Music song : list) {
			song.url = source(song.id);
			song.print();
		}
		return list;
	}

	@Override
	public List<Music> searchSimilar(String keyWord, int pageSize, int pn) {
		List<Music> list = searchWithLink(keyWord, pageSize, pn);
		for(int i = list.size() -1; i>=0; i--) {
			if(list.get(i).url == null)
				list.remove(i);
			list.get(i).print();
		}
		return list;
	}

	@Override
	public String source(String id) {
		try {
			String url = "https://music.163.com/weapi/song/enhance/player/url/?csrf_token=";
			HashMap<String, String> headers = new HashMap<>();
			headers.put("content-type", "application/x-www-form-urlencoded");
			headers.put("referer", "https://music.163.com/search/");
			headers.put("accept", "*/*");
			headers.put("origin", "https://music.163.com");
			headers.put("accept-encoding", "gzip, deflate, sdch, br");
			headers.put("accept-language", "zh-CN,zh;q=0.8");
			headers.put("user-agent",
					"Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 SE 2.X MetaSr 1.0");
			
			JSONObject obj = new JSONObject();
			obj.put("ids", new JSONArray().put(id));
			obj.put("br", 320000);
			obj.put("csrf_token", "");
			String params = NetEaseEncryptUtil.generateToken(obj.toString());
			String result = util.postContent(url, headers, params);
			//System.out.println(result);
			return new JSONObject(result).getJSONArray("data").getJSONObject(0).getString("url");
		} catch (Exception e) {
			return null;
		}
	}

}
