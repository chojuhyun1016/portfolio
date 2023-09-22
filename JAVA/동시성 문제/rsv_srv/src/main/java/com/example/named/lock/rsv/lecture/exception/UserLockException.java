package com.example.named.lock.rsv.lecture.exception;

public class UserLockException extends RuntimeException
{
	private static final long serialVersionUID = -8545431740744259720L;

	public UserLockException()
	{
		super("USER LOCK 을 수행하는 중에 오류가 발생하였습니다.");
	}

	public UserLockException(String str)
	{
		super(str);
	}
	
	public UserLockException(Throwable cause)
	{
		super(cause);
	}
}
