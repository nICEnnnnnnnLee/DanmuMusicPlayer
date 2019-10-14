package nicelee.function.music.source.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * params 参数计算 两次AES/CBC/Padding IV(0102030405060708) 两次加密结果均再次进行Base64编码
 * encSecKey 参数计算 RSA加密NoPadding
 * 
 * RSA的算法涉及三个参数，n、e1、e2。其中，n是两个大质数p、q的积，n的二进制表示时所占用的位数，就是所谓的密钥长度。
 * 
 * e1和e2是一对相关的值，e1可以任意取，
 * 
 * 要求e1与(p-1）*(q-1）互质；
 * 
 * 再选择e2，要求（e2*e1）mod((p-1）*(q-1））=1。
 * 
 * （n，e1）,(n，e2）就是密钥对。其中(n，e1）为公钥，(n，e2）为私钥。
 * 
 * RSA加解密的算法完全相同，设A为明文，B为密文，则：
 * 
 * A=B^e2 mod n；
 * 
 * B=A^e1 mod n；
 * 
 * @author Guan Yue
 * @time 2018年1月10日下午3:22:09
 *
 */
public class NetEaseEncryptUtil {
	private final static String IV = "0102030405060708";
	private static String randomStr = null;

	@SuppressWarnings("deprecation")
	public static String generateToken(String data) throws Exception {
		String param1 = data, param2 = "010001",
				param3 = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7",
				param4 = "0CoJUm6Qyw8W8jud";
		StringBuilder sb = new StringBuilder();
		String params = URLEncoder.encode(getParams(param1, param4));
		String encSecKey = URLEncoder.encode(getEncSecKey(param2, param3));
		sb.append("params=").append(params).append("&encSecKey=").append(encSecKey);
		return sb.toString();
	}

	public static String getMD5(String pass) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return byte2HexString(md.digest(pass.getBytes("utf-8")));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
		}
		return null;
	}

	/**
	 * copy net RSA加解密的算法完全相同，设A为明文，B为密文，则：公钥（e1,n）(pubkey,modulus)
	 * 
	 * A=B^e2 mod n；
	 * 
	 * B=A^e1 mod n；
	 * 
	 * @param pubKey
	 * @param modulus
	 * @return
	 */
	static String getencSecKey(String pubKey, String modulus) {
		String text = new StringBuilder(randomStr).reverse().toString();
		BigInteger rs = new BigInteger(String.format("%x", new BigInteger(1, text.getBytes())), 16)
				.modPow(new BigInteger(pubKey, 16), new BigInteger(modulus, 16));
		String r = rs.toString(16);
		if (r.length() >= 256) {
			return r.substring(r.length() - 256, r.length());
		} else {
			while (r.length() < 256) {
				r = 0 + r;
			}
			return r;
		}
	}

	/**
	 * OK!
	 * 
	 * 010001
	 * 00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7
	 * 
	 * @param param2
	 * @param param3
	 * @return
	 */
	public static String getEncSecKey(String param2, String param3) {
		try {
			final int MAX_ENCRYPT_BLOCK = 1024;
			BigInteger pubkey = new BigInteger(param2, 16);
			BigInteger modulus = new BigInteger(param3, 16);
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, pubkey);
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey publicKey = keyFactory.generatePublic(keySpec);

			// Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");// PKCS1Padding
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);

			randomStr = new StringBuilder(randomStr).reverse().toString();
			byte[] data = randomStr.getBytes("utf-8");
			int inputLen = data.length;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int offSet = 0;
			byte[] cache;
			int i = 0;

			// 对数据分段加密
			while (inputLen - offSet > 0) {
				if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
					cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
				} else {
					cache = cipher.doFinal(data, offSet, inputLen - offSet);
				}
				out.write(cache, 0, cache.length);
				i++;
				offSet = i * MAX_ENCRYPT_BLOCK;
			}
			byte[] encryptedData = out.toByteArray();
			out.close();
			String result = byte2HexString(encryptedData);
			if (result.length() >= 256) {
				return result.substring(result.length() - 256, result.length());
			} else {
				while (result.length() < 256) {
					result = 0 + result;
				}
				return result;
			}
		} catch (NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | IOException
				| InvalidKeySpecException | NoSuchPaddingException | InvalidKeyException e) {
			e.printStackTrace();
		} finally {
		}
		return null;
	}

	private static String byte2HexString(byte[] encryptedData) {
		StringBuilder sb = new StringBuilder(encryptedData.length);
		for (int i = 0; i < encryptedData.length; i++) {
			String temp = Integer.toHexString(encryptedData[i] & 0xff);
			if (temp.length() < 2)
				sb.append("0");
			sb.append(temp.toLowerCase());
		}
		return sb.toString();
	}

	/**
	 * 
	 * @param param1
	 * @param param4
	 * @return
	 */
	public static String getParams(String param1, String param4) {
		try {
			SecretKey key = getKey(param4.getBytes("utf-8"));
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");// PKCS5Padding
			cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV.getBytes("utf-8")));
			byte[] res = cipher.doFinal(param1.getBytes("utf-8"));
			res = Base64.getEncoder().encode(res);

			randomStr = getRandom(16);
			key = getKey(randomStr.getBytes("utf-8"));
			cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(IV.getBytes("utf-8")));
			byte[] result = cipher.doFinal(res);
			return new String(Base64.getEncoder().encode(result), "utf-8");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| InvalidAlgorithmParameterException | UnsupportedEncodingException | IllegalBlockSizeException
				| BadPaddingException e) {
			e.printStackTrace();
		} finally {

		}
		return null;
	}

	private static String getRandom(int i) {// 随机16字符即可
		StringBuilder sb = new StringBuilder(16);
		String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		for (int j = 0; j < 16; j++) {
			int m = (int) (Math.random() * alphabet.length());
			sb.append(alphabet.charAt(m));
		}
		return sb.toString();
	}

	private static SecretKey getKey(byte[] bytes) {
		byte[] keyTemp = new byte[16];
		for (int i = 0; i < keyTemp.length; i++) {
			keyTemp[i] = bytes[i];
		}
		return new SecretKeySpec(bytes, "AES");
	}

}
