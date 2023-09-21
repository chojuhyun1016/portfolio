package com.example.mp.gw.common.utils.enc;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.codec.binary.Base64;

/**
 * <pre>
 * Statements
 * </pre>
 *
 * @ClassName   : AES256Cipher.java
 * @Description : AES 256 암호화 복호화 클래스
 * @author      : 조주현
 * @since       : 2021. 3. 12.
 * @version 1.0
 * @see
 * @Modification Information
 * <pre>
 *     since          author              description
 *  ===========    =============    ===========================
 *  2021.3.12         조주현                최초 생성
 * </pre>
 */


public class AES256Cipher
{
	// 알고리즘/모드/패딩
	private static final String algorithm = "AES/CBC/PKCS5Padding";
	
	// 암호화 키
	private SecretKey secretKey;
	
	// 초기화 벡터
	private IvParameterSpec iv;
	
	// 문자인코딩 방식
	private final String charset = "UTF-8";
	
	public AES256Cipher (String aesKey)
	{
		if (null == aesKey)
		{
			throw new NoSecretKeyException("AESCipher().NoSecretKeyException : 암호화 시, secretKey 누락으로 인한 오류 발생");
		}
		
		if (aesKey.length() > 16)
		{
			this.iv = new IvParameterSpec(aesKey.substring(0, 16).getBytes());
			
		}
		else
		{
			this.iv = new IvParameterSpec(aesKey.getBytes());
		}
		
		this.secretKey = new SecretKeySpec(aesKey.getBytes(), "AES");
	}
	
	// 암호화
	public String encrypt(String str) throws Exception
	{
		Cipher c = Cipher.getInstance(algorithm);
		c.init(Cipher.ENCRYPT_MODE, this.secretKey, this.iv);
		
		return new String(Base64.encodeBase64(c.doFinal(str.getBytes(charset))));
	}
	
	// 복호화
	public String decrypt(String str) throws Exception
	{
		Cipher c = Cipher.getInstance(algorithm);
		c.init(Cipher.DECRYPT_MODE, this.secretKey, this.iv);
		
		return new String(c.doFinal(Base64.decodeBase64(str.getBytes())), charset);
	}
	
	
	class NoSecretKeyException extends RuntimeException
	{
		private static final long serialVersionUID = -4718385172418699875L;

		public NoSecretKeyException(String msg)
		{
			super(msg);
		}
	}
}
