#include <errno.h>
#include <fcntl.h>
#include <sys/msg.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "CNBaseReStart.h"
#include "CNBaseDefinitions.h"


/*******************************************************************************
* Update    : 2011/05/04                                                       *
* Argument  : char, int                                                        *
*             _cpFile : �� ������ ���/�����̸�                                *
*             _iMode  : ���Ͽ��� ���οɼ�                                      *
*                                                                              *
* Return    : int, ����(������ ��ũ����), ����(-1)                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �Ѱܹ��� ���ϸ�(_cpFile)�� �Ѱܹ��� ���(_iMode)�� ����          *
*             ��ũ���͸� ��ȯ�Ѵ�. �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������  *
*             ���鼭 ���Ͽ��⸦ ��õ��Ѵ�. ���ͷ�Ʈ �ܿ� �������� ���и�      *
*             �Ѵٸ� ������ �����ʰ� ���а�(-1)���� ��ȯ�Ѵ�.                  *
*******************************************************************************/
int r_open( const char* _cpFile, int _iMode )
{
	int	iResult;
	
	while( ( iResult = open( _cpFile, _iMode ) ) == CN_BASE_ERROR && errno == EINTR );
	
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int                                                              *
*             _iFd : �ݰ��� �ϴ� ������ ��ũ����                             *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �Ѱܹ��� ��ũ������ ������ �ݴ´�.                             *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 ���ϴݱ⸦       *
*             ��õ��Ѵ�. ���ͷ�Ʈ �ܿ� �������� ���и� �Ѵٸ� ������ �����ʰ� *
*             ���а�(-1)���� ��ȯ�Ѵ�.                                         *
*******************************************************************************/
int r_close( int _iFd )
{
	int	iResult;

	while( ( iResult = close( _iFd ) ) == CN_BASE_ERROR && errno == EINTR );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : char*, char*                                                     *
*             _cpFile : �� ������ ���/�����̸�                                *
*             _cpMode : ���Ͽ��� ���οɼ�                                      *
*                                                                              *
* Return    : int, ����(����������), ����(NULL)                                *
* Stability : MT-Safe                                                          *
* Explain   : �Ѱܹ��� ���ϸ�(_cpFile)�� �Ѱܹ��� ���(_cpMode)�� ����         *
*             ���������͸� ��ȯ�Ѵ�. �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������  *
*             ���鼭 ���Ͽ��⸦ ��õ��Ѵ�. ���ͷ�Ʈ �ܿ� �������� ���и�      *
*             �Ѵٸ� ������ �����ʰ� ���а�(NULL)���� ��ȯ�Ѵ�.                *
*******************************************************************************/
FILE* r_fopen( const char* _cpFile, char* _cpMode )
{
	FILE	*fpFile;
	
	while( ( fpFile = fopen( _cpFile, _cpMode ) ) == NULL && errno == EINTR );
	
	return	fpFile;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : FILE*                                                            *
*             _fpFile : �ݰ��� �ϴ� ������ ����������                          *
*                                                                              *
* Return    : int, ����(0), ����(EOF)                                          *
* Stability : MT-Safe                                                          *
* Explain   : �Ѱܹ��� ������������ ������ �ݴ´�.                             *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 ���ϴݱ⸦       *
*             ��õ��Ѵ�. ���ͷ�Ʈ �ܿ� �������� ���и� �Ѵٸ� ������ �����ʰ� *
*             ���а�(EOF)���� ��ȯ�Ѵ�.                                        *
*******************************************************************************/
int r_fclose( FILE* _fpFile )
{
	int	iResult;

	while( ( iResult = fclose( _fpFile ) ) != CN_BASE_SUCCESS && errno == EINTR );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : void*, size_t, size_t, FILE*                                     *
*             _vpBuf  : �о���� �����͸� ������ ������ �ּ�                   *
*             _iSize  : �о���� ������ �� Row�� ũ��                          *
*             _iCnt   : �о���� �������� Row ����                             *
*             _fpFile : �����͸� �о���� ������ ����������                    *
*                                                                              *
* Return    : int, ����(�о���� ����), ����(0)                                *
* Stability : MT-Safe                                                          *
* Explain   : ����(_fpFile)�κ��� _iSizeũ���� �����͸� _iCnt���� ��ŭ �о�鿩*
*             ����(_vpBuf)�� �����Ѵ�.                                         *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 _iCnt������ŭ    *
*             �о���϶� ���� �Դ����� �����Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ��      *
*             �ƴϰų� ������ ��� �о���� ������ ��ȯ�Ѵ�.                   *
*******************************************************************************/
size_t r_fread( void* _vpBuf, size_t _iSize,  size_t _iCnt, FILE* _fpFile )
{
	size_t	iResult;

	size_t	iCnt = _iCnt;
	size_t	iOffset = 0;

	while( ( iResult = fread( (char*)_vpBuf + iOffset, _iSize, iCnt, _fpFile ) ) != iCnt && errno == EINTR )
	{
		iCnt -= iResult;
		iOffset = ( _iCnt - iCnt ) * _iSize;
	}

	return	_iCnt - iCnt;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : void*, size_t, size_t, FILE*                                     *
*             _vpBuf  : ��� �� �����Ͱ� ����� ������ �ּ�                    *
*             _iSize  : ��� �� ������ �� Row�� ũ��                           *
*             _iCnt   : ��� �� �������� Row ����                              *
*             _fpFile : �����͸� ��� �� ������ ����������                     *
*                                                                              *
* Return    : int, ����(�о���� ����), ����(EOF)                              *
* Stability : MT-Safe                                                          *
* Explain   : ����(_vpBuf)�� _iSizeũ���� �����͸� _iCnt ������ŭ              *
*             ����(_fpFile)�� ����Ѵ�.                                        *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 _iCnt������ŭ    *
*             �������� ���⸦ �ݺ��Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ�� �ƴϰų�      *
*             ������ ��� ������ ������ ��ȯ�Ѵ�.                              *
*******************************************************************************/
size_t r_fwrite( void* _vpBuf, size_t _iSize,  size_t _iCnt, FILE* _fpFile )
{
	size_t	iResult;

	size_t	iCnt = _iCnt;
	size_t	iOffset = 0;

	while( ( iResult = fwrite( (char*)_vpBuf + iOffset, _iSize, iCnt, _fpFile ) ) != iCnt && errno == EINTR )
	{
		iCnt -= iResult;
		iOffset = ( _iCnt - iCnt ) * _iSize;
	}

	return	_iCnt - iCnt;
}


/*******************************************************************************
* Update    : 2011/05/04                                                       *
* Argument  : char*, int, FILE*                                                *
*             _cpBuf  : �о���� �����Ͱ� ���� �� ������ �ּ�                  *
*             _iSize  : �о���� �������� ũ��                                 *
*             _fpFile : �о���� ������ ���� ��ũ����                        *
*                                                                              *
* Return    : int, ����(�о���� ����), ����(EOF)                              *
* Stability : MT-Safe                                                          *
* Explain   : ����(_fpFile)���� �ִ� _iSizeũ������� �����͸� �о�鿩��      *
*             ����(_cpBuf)�� �����Ѵ�. fgets�� ���Ͽ��� �Ѷ���('\n')�� ������  *
*             ���� �о���̸� _iSize�� �ִ� �о���̴� �Ѷ��ΰ��� ����         *
*             ũ���̴�. ����(_cpBuf)�� ũ�⺸�� �Ѷ����� ũ�Ⱑ �� ����      *
*             �޸� �����÷ο�� ���� �޸� ħ���� �������ؼ��̴�.           *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 �б�(fgets)��    *
*             �ݺ��Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ�� �ƴϰų� ������ ��� NULL��   *
*             ��ȯ�Ѵ�.                                                        *
*******************************************************************************/
char* r_fgets( char* _cpBuf, int _iSize, FILE* _fpFile )
{
	char*	cpResult;

	while( ( cpResult = fgets( _cpBuf, _iSize, _fpFile ) ) == NULL && errno == EINTR );

	return	cpResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int                                                              *
*             _iFd  : ���� �� ���ϵ�ũ����                                   *
*                                                                              *
* Return    : int, ����(���� �� ��ũ����), ����(-1)                          *
* Stability : Async-Signal-Safe                                                *
* Explain   : �Ѱܹ��� ��ũ����(_iFd)�� �����ϰ� ���� �� ��ũ���͸�        *
*             ��ȯ�Ѵ�.                                                        *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 ���簡 �ɶ�����  *
*             ��õ��Ѵ�.                                                      *
*******************************************************************************/
int r_dup( int _iFd )
{
	int	iResult;
	
	while( ( iResult = dup( _iFd ) ) == CN_BASE_ERROR && errno == EINTR );
	
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int, int                                                         *
*             _iFd1  : �������� �޾Ƶ��� ��ũ����                            *
*             _iFd2  : ������ �� ��ũ����                                    *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : Async-Signal-Safe                                                *
* Explain   : �Ѱܹ��� ��ũ����(_iFd2)�� _iFd1���� ������ �Ѵ�.              *
*             ������ �̶� ��ũ���͸� �ٸ� ��ũ���ͷ� ������ ������ ������  *
*             �� ���� _iFd2�� ������� �Ѵٸ� _iFd1 ���� ������� �̷������.  *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 ������ �ɶ�����  *
*             ��õ��Ѵ�.                                                      *
*******************************************************************************/
int r_dup2( int _iFd1, int _iFd2 )
{
	int	iResult;
	
	while( ( iResult = dup2( _iFd1, _iFd2 ) ) == CN_BASE_ERROR && errno == EINTR );
	
	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : struct pollfd*, nfds_t, int                                      *
*             _stpPoll  : poll ����ü�� �ּ�                                   *
*             _iCnt     : �̺�Ʈ�� ������ ��ũ������ ����                    *
*             _iTimeout : Ÿ�Ӿƿ�                                             *
*                                                                              *
* Return    : int, ����(�̺�Ʈ�� �Ͼ ��ũ���� ����), ����(-1)             *
* Stability : Async-Signal-Safe                                                *
* Explain   : �Ѱ� ���� _stpPoll �����͸� �������� poll �Լ��� �����Ѵ�.       *
*             _iCnt���� ���� ��ũ���͸� �����ϸ� _iTimeout��ŭ ����Ѵ�.     *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 poll ������      *
*             �ݺ��Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ�� �ƴϰų� ������ ��� -1��     *
*             ��ȯ�Ѵ�.                                                        *
*******************************************************************************/
int	r_poll( struct  pollfd* _stpPoll, nfds_t _iCnt, int _iTimeout )
{
	int iResult;

	while( ( ( iResult = poll( _stpPoll, _iCnt, _iTimeout ) ) == CN_BASE_ERROR ) && ( errno == EINTR ) );

	return iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int, struct sockaddr*, nfds_t, socklen_t*                        *
*             _iListen     : accept������ ���� �� ������ ��ũ����            *
*             _stpSockAddr : accept�� ������ ������ ���� �� ����ü�� �ּ�      *
*             _ipLen       : _stpSockAddr ����ü�� ũ�Ⱚ�� �ּ�               *
*                                                                              *
* Return    : int, ����(���ӵ� ������ ��ũ����), ����(-1)                    *
* Stability : MT-Safe                                                          *
* Explain   : �Ѱ� ���� _iListen ���� accept ����(���ӿ�û ����)�� �����Ѵ�.   *
*             _stpSockAddr ����ü�� ���� �� ������ ������ ����ȴ�.            *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 accept ������    *
*             �ݺ��Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ�� �ƴϰų� ������ ��� -1��     *
*             ��ȯ�Ѵ�.                                                        *
*******************************************************************************/
int r_accept( int _iListen, struct sockaddr* _stpSockAddr, socklen_t* _ipLen )
{
	int iResult;

	while( ( ( iResult = accept( _iListen, _stpSockAddr, _ipLen ) ) == CN_BASE_ERROR ) && (errno == EINTR) );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : const sigset_t*, siginfo_t*                                      *
*             _stpSigSet  : �޾Ƶ��� �ñ׳��� ���� �� ����ü�� �ּ�            *
*             _stpSigInfo : �޾Ƶ鿩�� �ñ׳��� ������ ���� �� ����ü�� �ּ�   *
*                                                                              *
* Return    : int, ����(�ñ׳� ��ȣ), ����(-1)                                 *
* Stability : MT-Safe                                                          *
* Explain   : _stpSigSet �ּ��� ����ü�� ���� �� �ñ׳��� �߻� �� ������       *
*             ��ٸ��� �ش� �ñ׳��� �߻� �� ��� siginfo_t ����ü�� �ñ׳ο�  *
*             ���� ������ �����Ѵ�.                                            *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 sigwaitinfo      *
*             ������ �ݺ��Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ�� �ƴϰų� ������ ���   *
*             -1�� ��ȯ�Ѵ�.                                                   *
*******************************************************************************/
int	r_sigwaitinfo( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo )
{
	int	iResult;

	while( ( ( iResult = sigwaitinfo( _stpSigSet, _stpSigInfo ) ) == CN_BASE_ERROR ) && ( errno == EINTR ) );

	return	iResult;

}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : const sigset_t*, siginfo_t*, const struct timespec*              *
*             _stpSigSet  : �޾Ƶ��� �ñ׳��� ���� �� ����ü�� �ּ�            *
*             _stpSigInfo : �޾Ƶ鿩�� �ñ׳��� ������ ���� �� ����ü�� �ּ�   *
*             _stpTsTime  : �ñ׳� ���ð�                                    *
*                                                                              *
* Return    : int, ����(�ñ׳� ��ȣ), ����(-1)                                 *
* Stability : MT-Safe                                                          *
* Explain   : _stpSigSet �ּ��� ����ü�� ���� �� �ñ׳��� _stpTsTime �ּ���    *
*             ����ü�� ���� �� �ð���ŭ ��ٸ���.                              *
*             �ñ׳��� �߻� �� ��� siginfo_t ����ü�� �ñ׳ο� ���� ������    *
*             �����ϰ� �ñ׳� ��ȣ�� ��ȯ�Ѵ�.                                 *
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 sigwaitinfo      *
*             ������ �ݺ��Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ�� �ƴϰų� ������ ���   *
*             -1�� ��ȯ�Ѵ�. Ÿ�Ӿƿ��� ��� -1�� ��ȯ�ϸ� errno�� EAGIN����   *
*             ���õȴ�.                                                        *
*******************************************************************************/
int	r_sigtimedwait( const sigset_t* _stpSigSet, siginfo_t* _stpSigInfo, const struct timespec* _stpTsTime )
{
	int	iResult;

	while( ( ( iResult = sigtimedwait( _stpSigSet, _stpSigInfo, _stpTsTime ) ) == CN_BASE_ERROR ) && ( errno == EINTR ) );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int, void*, size_t, int                                          *
*             _iQueueID    : �����͸� ������ Queue�� ID                        *
*             _vpPacket    : ���� �� �����Ͱ� ���� �� ����                     *
*             _iPacketSize : ���� �� �������� ũ��                             *
*             _iMsgFlag    : �б� ���οɼ�(����/�����)                        *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : _iQueueID�� �ش��ϴ� Queue�� ����(_vpPacket)�� �����͸�          *
*             _iPacketSize ������ ��ŭ ����Ѵ�. _iMsgFlag�� �޽��� ���� �ɼ�  *
*             ���ν� ���۸�带 ����� �Ұ����� ������ �Ұ����� �����Ѵ�.    * 
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 msgsnd           *
*             ������ �ݺ��Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ�� �ƴϰų� ������ ���   *
*             -1�� ��ȯ�Ѵ�.                                                   *
*******************************************************************************/
int	r_msgsnd( int _iQueueID, void* _vpPacket,  size_t _iPacketSize, int _iMsgFlag )
{
	int	iResult;

	while( ( iResult = msgsnd( _iQueueID, _vpPacket, _iPacketSize, _iMsgFlag ) ) == CN_BASE_ERROR && errno == EINTR );

	return	iResult;
}


/*******************************************************************************
* Update    : 2011/05/03                                                       *
* Argument  : int, void*, size_t, long, int                                    *
*             _iQueueID    : �����͸� �о���� Queue�� ID                      *
*             _vpPacket    : �о���� �����͸� ���� �� ����                    *
*             _iPacketSize : �о���� �����͸� ���� �� ������ ũ��             *
*             _iMsgType    : �о���� �޽����� Ÿ��                            *
*             _iMsgFlag    : �б� ���οɼ�(����/�����)                        *
*                                                                              *
* Return    : int, ����(0), ����(-1)                                           *
* Stability : MT-Safe                                                          *
* Explain   : _iQueueID�� �ش��ϴ� Queue���� _iMsgTypeŸ���� �����͸� �о   *
*             ����(_vpPacket)�� �����Ѵ�. _iPacketSize�� ����(_vpPacket)��     *
*             ũ���̸� ������ ũ��� �׻� �о���� �����ͺ��� Ŀ���Ѵ�.        *
*             _iMsgFlag�� �޽��� ���� �ɼ� ���ν� ���۸�带 ����� �Ұ�����   *
*             ������ �Ұ����� �����Ѵ�.                                      * 
*             �ñ׳ο����� ���ͷ�Ʈ �Ǿ��� ��� ������ ���鼭 msgrcv           *
*             ������ �ݺ��Ѵ�. �ñ׳ο� ���� ���ͷ�Ʈ�� �ƴϰų� ������ ���   *
*             -1�� ��ȯ�Ѵ�.                                                   *
*******************************************************************************/
int	r_msgrcv( int _iQueueID, void* _vpPacket,  size_t _iPacketSize, long _iMsgType, int _iMsgFlag )
{
	int	iResult;

	while( ( iResult = msgrcv( _iQueueID, _vpPacket, _iPacketSize, _iMsgType, _iMsgFlag ) ) == CN_BASE_ERROR && errno == EINTR );

	return	iResult;
}
