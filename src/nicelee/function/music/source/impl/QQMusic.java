package nicelee.function.music.source.impl;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import nicelee.common.annotation.Autoload;
import nicelee.common.util.HttpRequestUtil;
import nicelee.function.music.domain.Music;
import nicelee.function.music.source.IMusicAPI;

@Autoload(source = "qq")
public class QQMusic implements IMusicAPI {

	HttpRequestUtil util;

	public QQMusic() {
		util = new HttpRequestUtil();
	}

	@Override
	public List<Music> searchWithoutLink(String keyWord, int pageSize, int pn) {
		List<Music> list = new ArrayList<Music>();
		try {
			String url;

			url = String.format(
					"https://c.y.qq.com/soso/fcgi-bin/client_search_cp?ct=24&qqmusic_ver=1298&new_json=1&t=0&aggr=1&cr=1&p=%d&n=%d&w=%s",
					pn, pageSize, URLEncoder.encode(keyWord, "UTF-8"));
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Accept", "*/*");
			headers.put("Accept-Encoding", "gzip, deflate, sdch, br");
			headers.put("Accept-Language", "zh-CN,zh;q=0.8");
			headers.put("Content-Type", "application/json;charset=UTF-8");
			headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64)");
			headers.put("Referer", "https://y.qq.com/portal/player.html");
			String result = util.getContent(url, headers);
			result = result.substring(9, result.length() - 1);
			//System.out.println(result);

			JSONArray jsonArr = new JSONObject(result).getJSONObject("data").getJSONObject("song").getJSONArray("list");
			for (int i = 0; i < jsonArr.length(); i++) {
				// System.out.printf(" 当前第一个歌曲%d信息:\n",i+1);
				Music song = new Music();
				JSONObject json = jsonArr.getJSONObject(i);
				song.id = json.getString("mid");
				song.name = json.getString("name");
				song.singer = json.getJSONArray("singer").getJSONObject(0).getString("name");
				song.album = json.getJSONObject("album").getString("name");
				// song.print();
				list.add(song);
			}

		} catch (Exception e) {
		}
		return list;
	}

	@Override
	public List<Music> searchWithLink(String keyWord, int pageSize, int pn) {
		List<Music> list = searchWithoutLink(keyWord, pageSize, pn);
		for (Music song : list) {
			song.url = source(song.id);
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
			String url = "https://u.y.qq.com/cgi-bin/musicu.fcg?data=";
			String param = String.format(
					"{\"req_0\":{\"module\":\"vkey.GetVkeyServer\",\"method\":\"CgiGetVkey\",\"param\":{\"guid\":\"12345678\",\"songmid\":[\"%s\"],\"songtype\":[0],\"uin\":\"0\",\"loginflag\":1,\"platform\":\"20\"}},\"comm\":{\"uin\":0,\"format\":\"json\",\"ct\":20,\"cv\":0}}",
					id);
			url += URLEncoder.encode(param, "UTF-8");
			HashMap<String, String> headers = new HashMap<>();
			headers.put("Accept", "*/*");
			headers.put("Accept-Encoding", "gzip, deflate, sdch, br");
			headers.put("Accept-Language", "zh-CN,zh;q=0.8");
			headers.put("Content-Type", "application/json;charset=UTF-8");
			headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64)");
			headers.put("Referer", "https://y.qq.com/portal/player.html");
			String result = util.getContent(url, headers);
			// System.out.println(result);
			JSONObject jObj = new JSONObject(result).getJSONObject("req_0").getJSONObject("data");
			String purl = jObj.getJSONArray("midurlinfo").getJSONObject(0).getString("purl");
			if (purl.isEmpty()) {
				return null;
			}
			String sip = jObj.getJSONArray("sip").getString(0);
			// System.out.println(sip + purl);
			return sip + purl;
		} catch (Exception e) {
			return null;
		}
	}
}
