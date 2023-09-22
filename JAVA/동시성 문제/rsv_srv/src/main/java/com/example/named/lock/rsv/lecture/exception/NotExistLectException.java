package com.example.named.lock.rsv.lecture.exception;

public class NotExistLectException extends RuntimeException
{
	private static final long serialVersionUID = 5133782788490870064L;

	public NotExistLectException()
	{
		super("존재하지 않는 강연입니다.");
	}

	public NotExistLectException(Throwable cause)
	{
		super(cause);
	}
}
