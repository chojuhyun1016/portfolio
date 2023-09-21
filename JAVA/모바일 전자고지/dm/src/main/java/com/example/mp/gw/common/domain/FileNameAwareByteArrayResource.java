package com.example.mp.gw.common.domain;


import org.springframework.core.io.ByteArrayResource;

/**
 * @Class Name : FileNameAwareByteArrayResource.java
 * @Description : Multipart form 파일 전송 요청을 wrapping 하는 객체
 * 
 * @author 조주현
 * @since 2021.05.06
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.05.06	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FileNameAwareByteArrayResource extends ByteArrayResource
{
    private String fileName;


    public FileNameAwareByteArrayResource(String fileName, byte[] byteArray, String description)
    {
        super(byteArray, description);
        this.fileName = fileName;
    }

    @Override
    public String getFilename()
    {
        return fileName;
    }
}
