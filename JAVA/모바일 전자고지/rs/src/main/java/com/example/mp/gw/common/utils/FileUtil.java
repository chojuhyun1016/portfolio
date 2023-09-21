package com.example.mp.gw.common.utils;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : FileUtil.java
 * @Description :  파일 유틸리티
 * 
 * @author 조주현
 * @since 2021.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
public class FileUtil
{
	/**
	 * 
	 * @param pdfList  파일 명 목록
	 * @param filePath 파일 경로
	 * @return
	 */
	public static byte[] createZipFile(String[] pdfList, String filePath)
	{
		byte[] buf = new byte[1024];
		ZipOutputStream outputStream = null;
		FileInputStream fileInputStream = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		try
		{
			outputStream = new ZipOutputStream(baos);
			
			for (String fileName : pdfList)
			{
				fileInputStream = new FileInputStream(filePath + fileName);
				ZipEntry ze = new ZipEntry(fileName);
				outputStream.putNextEntry(ze);
				int length = 0;
				
				while (0 < (length = fileInputStream.read(buf)))
				{
					outputStream.write(buf, 0, length);
				}
				
				outputStream.closeEntry(); 
				fileInputStream.close();
			}
			
			outputStream.close();
		}
		catch (IOException e)
		{
			
		}
		finally
		{
			try
			{
				for (int i = 0 ; i < pdfList.length ; i++)
				{
					File file = new File(filePath + pdfList[i]);
					
					if (file.exists())
					{
						if (file.delete())
						{
							log.debug("파일 삭제 성공");
						}
						else
						{
							log.debug("파일 삭제 실패");
						}
					}
				}
				
				outputStream.closeEntry();
				outputStream.close();
				fileInputStream.close();
			}
			catch(IOException e)
			{
			}
		}
		
		return baos.toByteArray();
	}
	
	public static void writeToFile(String filePath, byte[] pData)
	{
		if (null == pData)
		{
			return;
		}
		
		File lOutFile = new File(filePath);
		
		try
		{
			FileOutputStream lFileOutputStream = new FileOutputStream(lOutFile);
			lFileOutputStream.write(pData);
			lFileOutputStream.close();
		}
		catch (IOException e)
		{
		}
	}
}
