package com.example.mp.gw.common.utils.enc;


import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * <pre>
 * Statements
 * </pre>
 *
 * @ClassName   : AES128Cipher.java
 * @Description : AES 128 암호화 복호화 클래스
 * @author      : 조주현
 * @since       : 2021. 3. 12.
 * @version 1.0
 * @see
 * @Modification Information
 * <pre>
 *     since          author              description
 *  ===========    =============    ===========================
 *  2021.3.12.        조주현                최초 생성
 * </pre>
 */


public class AES128Cipher
{
	/**
	 * hex to byte[] : 16진수 문자열을 바이트 배열로 변환한다.
	 *
	 * @param hex
	 *            hex string
	 * @return
	 */
	public static byte[] hexToByteArray(String hex)
	{
		if (null == hex || 0 == hex.length())
		{
			return null;
		}
		
		byte[] ba = new byte[hex.length() / 2];
		
		for (int i = 0; i < ba.length; i++)
		{
			ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}

		return ba;
	}

	/**
	 * byte[] to hex : unsigned byte(바이트) 배열을 16진수 문자열로 바꾼다.
	 *
	 * @param ba
	 *            byte[]
	 * @return
	 */
	public static String byteArrayToHex(byte[] ba)
	{
		if (null == ba || 0 == ba.length)
		{
			return null;
		}

		StringBuffer sb = new StringBuffer(ba.length * 2);
		String hexNumber;

		for (int x = 0; x < ba.length; x++)
		{
			hexNumber = "0" + Integer.toHexString(0xff & ba[x]);
			sb.append(hexNumber.substring(hexNumber.length() - 2));
		}

		return sb.toString();
	}

	/**
	 * AES 방식의 암호화
	 *
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public static String encrypt(String message, String key, String iv) throws Exception
	{
		// use key coss2
		SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
		// Instantiate the cipher
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec, new IvParameterSpec(iv.getBytes()));
		byte[] encrypted = cipher.doFinal(message.getBytes());
		return byteArrayToHex(encrypted);
	}

	/**
	 * AES 방식의 복호화
	 *
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public static String decrypt(String encrypted, String key, String iv) throws Exception
	{
		// use key coss2
		SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(), "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, new IvParameterSpec(iv.getBytes()));
		byte[] original = cipher.doFinal(hexToByteArray(encrypted));
		String originalString = new String(original);
		return originalString;
	}
}
