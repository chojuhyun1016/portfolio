package com.example.mp.gw.common.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import com.example.mp.gw.common.exception.MalformedException;
import com.example.mp.gw.common.utils.ResponseBuilder;

/**
 * @Class Name : MapValidationErrorService.java
 * @Description : validation 에러 컨버터 서비스 
 * 
 * @author 조주현
 * @since 2021.04.23
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.23	    조주현          최초 생성
 * 
 *      </pre>
 * 
 */


@Service
public class MapValidationErrorService
{
	@SuppressWarnings("unchecked")
	public ResponseEntity<?> mapValidation(BindingResult result)
	{
		if (result.hasErrors())
		{
			List<Map<String,Object>> listMap = new ArrayList<Map<String,Object>>();
			List<String> errorStringList = new ArrayList<String>();
			Map<String, Object> errorMap = null;

			for (FieldError fieldError : result.getFieldErrors())
			{
				errorMap = new HashMap<>();
				String field = fieldError.getField();
				String errorMessage = fieldError.getDefaultMessage();
				List<Map<String, Object>> prevList = null;
				Map<String, Object> prevMap = errorMap;
				Object prevValue = null;
				int t = 1;

				if (-1 < field.indexOf("."))
				{
					String[] splitedFields = field.split("\\.");

					for (String splitedField : splitedFields)
					{
						String[] keyOrIndex = splitedField.split("\\[");

						if (1 == keyOrIndex.length)
						{
							if(t == splitedFields.length)
							{
								prevMap.put(keyOrIndex[0], errorMessage);
							}
							else
							{
								Map<String, Object> innerMap = new HashMap<>();
								prevMap.put(keyOrIndex[0], innerMap);
								prevMap = innerMap;
							}
						}
						else
						{
							prevValue = prevMap.get(keyOrIndex[0]);

							if (null == prevValue)
							{
								prevList = new ArrayList<>();
							}
							else
							{
								prevList = (List<Map<String, Object>>) prevValue;
							}

							String index = keyOrIndex[1].replaceAll("\\]", "");

							for (int i = 0; i < Integer.parseInt(index); i++)
							{
								try
								{
									prevList.get(i);
								}
								catch (RuntimeException e)
								{
									prevList.add(i, new HashMap<>());
								}
							}

							prevMap.put(keyOrIndex[0], prevList);

							try
							{
								prevMap = prevList.get(Integer.parseInt(index));
							}
							catch (RuntimeException e)
							{
								prevMap = new HashMap<>();
								prevList.add(Integer.parseInt(index), prevMap);
							}
						}

						t++;
					}
				}
				else
				{
					errorStringList.add(errorMessage);
				}
			}

			if (errorStringList.size() > 0 )
			{
				for (String getStr : errorStringList)
				{
					errorMap = new HashMap<>();
					errorMap.put("error_msg", getStr);
					listMap.add(errorMap);
					errorMap = null;
				}
			}

			return ResponseBuilder.response(listMap, new MalformedException());
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	public ResponseEntity<?> mapValidation(BindingResult result, Exception err)
	{
		if (result.hasErrors())
		{
			List<Map<String,Object>> listMap = new ArrayList<Map<String,Object>>();
			List<String> errorStringList = new ArrayList<String>();			
			Map<String, Object> errorMap = new HashMap<>();

			for (FieldError fieldError : result.getFieldErrors())
			{
				String field = fieldError.getField();
				String errorMessage = fieldError.getDefaultMessage();
				List<Map<String, Object>> prevList = null;
				Map<String, Object> prevMap = errorMap;
				Object prevValue = null;
				int t = 1;

				if (-1 < field.indexOf("."))
				{
					String[] splitedFields = field.split("\\.");
					for (String splitedField : splitedFields)
					{
						String[] keyOrIndex = splitedField.split("\\[");

						if (1 == keyOrIndex.length)
						{
							if (t == splitedFields.length)
							{
								prevMap.put(keyOrIndex[0], errorMessage);
							}
							else
							{
								Map<String, Object> innerMap = new HashMap<>();
								prevMap.put(keyOrIndex[0], innerMap);
								prevMap = innerMap;
							}
						}
						else
						{
							prevValue = prevMap.get(keyOrIndex[0]);
							if (null == prevValue)
							{
								prevList = new ArrayList<>();
							}
							else
							{
								prevList = (List<Map<String, Object>>) prevValue;
							}

							String index = keyOrIndex[1].replaceAll("\\]", "");

							for (int i = 0; i < Integer.parseInt(index); i++)
							{
								try
								{
									prevList.get(i);
								}
								catch (RuntimeException e)
								{
									prevList.add(i, new HashMap<>());
								}
							}

							prevMap.put(keyOrIndex[0], prevList);

							try
							{
								prevMap = prevList.get(Integer.parseInt(index));
							}
							catch (RuntimeException e)
							{
								prevMap = new HashMap<>();
								prevList.add(Integer.parseInt(index), prevMap);
							}
						}

						t++;
					}
				}
				else
				{
					errorStringList.add(errorMessage);
				}
			}

			if (errorStringList.size() >0 )
			{
				for (String getStr : errorStringList)
				{
					errorMap = new HashMap<>();
					errorMap.put("error_msg", getStr);
					listMap.add(errorMap);
					errorMap = null;
				}
			}

			return ResponseBuilder.response(listMap, err);
		}

		return null;
	}
}
