package com.example.named.lock.rsv.lecture.exception;

public class OverMaxApplException extends RuntimeException
{
	private static final long serialVersionUID = 1830281351944482434L;

	public OverMaxApplException()
	{
		super("등록 가능 인원을 초과하였습니다.");
	}

	public OverMaxApplException(Throwable cause)
	{
		super(cause);
	}
}
