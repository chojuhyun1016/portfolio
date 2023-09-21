package com.example.mp.gw.kisa.exception;


public class FailReponseRegisterToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 6140743911797176863L;

	public FailReponseRegisterToKisaException()
	{
		super("KISA 공인전자주소 등록 결과 실패입니다");
	}

	public FailReponseRegisterToKisaException(Throwable cause)
	{
		super(cause);
	}
}
