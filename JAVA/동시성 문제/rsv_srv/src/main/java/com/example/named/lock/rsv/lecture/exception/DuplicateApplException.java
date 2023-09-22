package com.example.named.lock.rsv.lecture.exception;

public class DuplicateApplException extends RuntimeException
{
	private static final long serialVersionUID = 5051447299846280997L;

	public DuplicateApplException()
	{
		super("이미 등록한 강연입니다.");
	}

	public DuplicateApplException(Throwable cause)
	{
		super(cause);
	}
}
