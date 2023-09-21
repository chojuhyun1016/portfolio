package com.example.mp.gw.common.domain;


import java.io.IOException;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : FileInfo.java
 * @Description : 파일정보 객체 
 * 
 * @author 조주현
 * @since 2021.04.05
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.05	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@ToString(exclude = {"fileData"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileInfo
{
	private byte[]  fileData;
	private String  fileName;
	private Integer fileSize;
	private String  fileExt;

	public FileInfo(MultipartFile file) throws IOException
	{
		fileData = file.getBytes();
		fileName = file.getOriginalFilename();
		fileSize = (int)file.getSize();
		fileExt  = StringUtils.hasText(fileName) ? fileName.substring(fileName.lastIndexOf(".") + 1) : "";
	}
}
