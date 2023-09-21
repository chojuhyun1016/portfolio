package com.example.mp.gw.maintain.domain;


import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @Class Name : WorkerInfo.java
 * @Description : WorkerInfo 객체 
 * 
 * @author 조주현
 * @since 2023.03.14
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.14	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Data
@Setter
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class WorkerInfo
{
	// 서버 프리픽스("1"/"2")
	private String prefix;

	// 업데이트 시간("YYYYMMDDhh24miss")
	private String updatedDtm;

	// 동작 여부(["Y"/"N","Y"/"N"])
	@Builder.Default
	private List<String> running = new ArrayList<String>();
}
