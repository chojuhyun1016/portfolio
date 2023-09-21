package com.example.mp.gw.common.utils.enc;


import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.mp.gw.common.domain.Const;
import com.penta.scpdb.ScpDbAgent;
import com.penta.scpdb.ScpDbAgentException;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : Encryptor.java
 * @Description : 암호화를 위한 유틸
 * 
 * @author 조주현
 * @since 2021.03.26
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.26	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
@Component
public class Encryptor
{
	@Autowired
    ApplicationContext context;

	/** 암호화 키 */
	private String encryptKey;
	private String encryptIv;

	private static Key    key = null; 
	private static String iv  = null;


	@PostConstruct
	public void postConstruct()
	{
    	ConfigurableEnvironment environment = (ConfigurableEnvironment)context.getEnvironment();

        try
        {
        	if (!"local".equals(environment.getProperty("spring.profiles")))
        	{
        		// DAMO SCP API 설정 파일과 키파일의 풀 패스
				String iniFilePath = environment.getProperty("spring.damo.scpdbAgentPath"); // scpdb_agent.ini fullpath

				// DAMO SCP API ScpDbAgent 객체를 생성
				ScpDbAgent agt = new ScpDbAgent();

				// KMS에 주입한 외부키를 가져오는 메소드
				String outKey   = agt.ScpExportKey(iniFilePath, "KEY1", " ");
				byte[] hexBytes = new byte[outKey.length() / 2];

				for (int i = 0, j = 0; i < outKey.length(); i += 2)
				{
					hexBytes[j++] = Byte.parseByte(outKey.substring(i, i + 2), 16);
				}

				String orikey = new String(hexBytes);

				// 8배수 끝 4자리 9999 자르기
				orikey = orikey.split("9999")[0];
            	
            	log.info("orikey : [" + orikey + "]");

            	encryptKey = orikey;
            	encryptIv  = outKey.substring(0, 16);
        	}
        }
        catch (ScpDbAgentException e1)
        {
			log.error("EncryptEnvPostProcessor postProcessEnvironment ScpDbAgentException: {}", e1.getMessage());
		}
        catch (Exception e)
        {
			log.error("EncryptEnvPostProcessor postProcessEnvironment Exception: {}", e.getMessage());
        }
		
    	key = new SecretKeySpec(Base64.getDecoder().decode(encryptKey), "AES");
    	iv = encryptIv;
	}
	
	public static String encryptPhone(String phone) throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		Cipher cipher   = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] enc_buff = encrypt(phone.getBytes(), cipher);

		if (null == enc_buff)
			return null;

		return new String(Base64.getEncoder().encode(enc_buff));
	}
	
	public static String decryptPhone(String phone) throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		String ctn;
		StringBuffer sb = new StringBuffer();

		Cipher cipher   = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] dec_buff = decrypt(Base64.getDecoder().decode(phone), cipher);

		if (null == dec_buff)
			return null;
        
        for( int i = 0; i < dec_buff.length; i++ )
        {
        	sb.append((char)dec_buff[i]);
        }

        ctn = sb.toString();

        sb.delete(0, sb.length());
		
		return ctn;
	}

	public static String encryptCi(String ci) throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		Cipher cipher   = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] enc_buff = encrypt(ci.getBytes(), cipher);

		if (null == enc_buff)
			return null;

		return new String(Base64.getEncoder().encode(enc_buff));
	}
	
	public static String decryptCi(String ci) throws NoSuchAlgorithmException, NoSuchPaddingException
	{
		String ctn;
		StringBuffer sb = new StringBuffer();

		Cipher cipher   = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] dec_buff = decrypt(Base64.getDecoder().decode(ci), cipher);

		if (null == dec_buff)
			return null;
        
        for (int i = 0; i < dec_buff.length; i++)
        {
        	sb.append( (char)dec_buff[i] );
        }

        ctn = sb.toString();

        sb.delete(0, sb.length());
		
		return ctn;
	}
	
	public static byte[] encrypt(byte[] data,Cipher cipher)
	{
		try
		{
			cipher.init(1, key, new IvParameterSpec(iv.getBytes()));

			return cipher.doFinal(data);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static byte[] decrypt(byte[] data,Cipher cipher)
	{
		try
		{
			cipher.init(2, key, new IvParameterSpec(iv.getBytes()));

			return cipher.doFinal(data);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public static String encrypt(String target, String type) throws NoSuchAlgorithmException
	{
		if(!StringUtils.hasText(target))
			return target;

		MessageDigest md = MessageDigest.getInstance(type);
		md.update(target.getBytes());

		StringBuilder builder = new StringBuilder();

	    for (byte b: md.digest())
	    {
	      builder.append(String.format("%02x", b));
	    }

	    String result = builder.toString();

	    return result;
	}
	
	// <FIXME:공인전자주소 생성 (SHA 256 암호화 -> Base64 인코딩)>
	public static String generateCea(String ci) throws NoSuchAlgorithmException
	{
		// SHA-256으로 해싱
		MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update((ci + Const.LGUPLUS.CEA_SUFFIX).getBytes());
		
        // 바이트를 헥스값으로 변환
        byte[]        bytes   = md.digest();        
        StringBuilder builder = new StringBuilder();

        for (byte b: bytes)
        {
          builder.append(String.format("%02x", b));
        }

        return builder.toString();
	}
}
