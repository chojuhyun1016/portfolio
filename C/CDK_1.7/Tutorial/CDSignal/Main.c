#include <time.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/atomic.h>

#include "Main.h"


int main( int argc, char **argv )
{
	int					iResult;

	// �ñ׳� ���� ���� ��ü
	sigset_t			stTempSigSet1;
	sigset_t			stTempSigSet2;

	// �ñ׳� ���� ��ü
	siginfo_t			stSigInfo1;
	siginfo_t			stSigInfo2;

	timespec			stTimeSpec;

	// �ñ׳� ��� ��ü
	struct sigaction	stTempSigAction;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// Ư�� �Լ�(SignalHandler1)�� �ñ׳� ó�� �Լ��� ���
	::fprintf( stderr, "\n[CDSignalInit() Start][L:%d]\n", __LINE__ );

	if( CDSignalInit( SignalHandler1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalInit( %d ) Error][E:%d][L:%d]\n", 
			SignalHandler1, errno, __LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), 15 );

	::fprintf( stderr, "[CDSignalInit() End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalAction() Start][L:%d]\n", __LINE__ );

	// Ư�� �Լ�(SignalHandler2)�� Ư�� �ñ׳�(SIGUSR1) ó�� �Լ��� ���
	// ������ �ñ׳� ������ stTempSigAction(�ñ׳� ��� ��ü)�� ����
	if( CDSignalAction( SIGUSR1, SignalHandler2, &stTempSigAction ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalAction( %d %d %d ) Error][E:%d][L:%d]\n", 
			SIGUSR1, 
			SignalHandler2, 
			&stTempSigAction, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), SIGUSR1 );

	::fprintf( stderr, "[CDSignalAction() End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalIgnore() Start][L:%d]\n", __LINE__ );

	// �����尡 Ư�� �ñ׳�(SIGUSR1)�� �����ϵ��� ����
	// ������ �ñ׳� ������ stTempSigAction(�ñ׳� ��� ��ü)�� ����
	if( CDSignalIgnore( SIGUSR1, &stTempSigAction ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalIgnore( %d %d ) Error][E:%d][L:%d]\n", 
			SIGUSR1, 
			&stTempSigAction, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), SIGUSR1 );

	::fprintf( stderr, "[CDSignalIgnore() End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalInitRts() Start][L:%d]\n", __LINE__ );

	// Ư�� �Լ�(RtSignalHandler1)�� RTS(�ǽð� Ȯ�� �ñ׳�) ó�� �Լ��� ���
	if( CDSignalInitRts( RtSignalHandler1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalInitRts( %d ) Error][E:%d][L:%d]\n", 
			RtSignalHandler1, errno, __LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), 15 );

	::fprintf( stderr, "[CDSignalInitRts() End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalRtsAction() Start][L:%d]\n", __LINE__ );

	// Ư�� �ñ׳�(SIGUSR2)�� RTS(�ǽð� Ȯ�� �ñ׳�) ó�� �Լ���
	// ó�� �ϵ��� �ñ׳� ó�� �Լ�(RtSignalHandler2)�� ���
	// ������ �ñ׳� ������ stTempSigAction(�ñ׳� ��� ��ü)�� ����
	if( CDSignalRtsAction( SIGUSR2, RtSignalHandler2, &stTempSigAction ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalRtsAction( %d %d %d ) Error][E:%d][L:%d]\n", 
			SIGUSR2, 
			RtSignalHandler2, 
			&stTempSigAction, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), SIGUSR2 );

	::fprintf( stderr, "[CDSignalRtsAction() End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalIgnore() Start][L:%d]\n", __LINE__ );

	// �����尡 Ư�� �ñ׳�(SIGUSR1)�� �����ϵ��� ����
	// ������ �ñ׳� ������ stTempSigAction(�ñ׳� ��� ��ü)�� ����
	if( CDSignalIgnore( SIGUSR2, &stTempSigAction ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalIgnore( %d %d %d ) Error][E:%d][L:%d]\n", 
			SIGUSR2, 
			&stTempSigAction, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), SIGUSR2 );

	::fprintf( stderr, "[CDSignalIgnore() End][L:%d]\n", __LINE__ );

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "\n\n[CDSignalInit Start][L:%d]\n", __LINE__ );

	// Ư�� �Լ�(SignalHandler1)�� �ñ׳� ó�� �Լ��� ���
	if( CDSignalInit( SignalHandler1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalInit( %d ) Error][E:%d][L:%d]\n", 
			SignalHandler1, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[CDSignalInit End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[sigemptyset Start][L:%d]\n", __LINE__ );

	// �ñ׳� ���� ���� ��ü(stTempSigSet1) ���� �����͸�
	// ��� �ʱ�ȭ(��ϵ� �ñ׳��� ����)
	if( ::sigemptyset( &stTempSigSet1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[sigemptyset( %d ) Error][E:%d][L:%d]\n", 
			&stTempSigSet1, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[sigemptyset End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[sigaddset Start][L:%d]\n", __LINE__ );

	// �ñ׳� ���� ���� ��ü(stTempSigSet1)�ȿ� Ư�� �ñ׳�(SIGUSR1) �߰�
	if( ::sigaddset( &stTempSigSet1, SIGUSR1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[sigaddset( %d %d ) Error][E:%d][L:%d]\n", 
			&stTempSigSet1, 
			SIGUSR1, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[sigaddset End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalMask Start][L:%d]\n", __LINE__ );

	// stTempSigSet1(�ñ׳� ���� ���� ��ü) ���� �ñ׳��� ����(SIG_BLOCK) �ϵ��� ����
	// ���� ������ ���� ������ stTempSigSet2(�ñ׳� ���� ���� ��ü) �ȿ� ����
	if( CDSignalMask( SIG_BLOCK, &stTempSigSet1, &stTempSigSet2 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[sigaddset( %d %d %d ) Error][E:%d][L:%d]\n", 
			SIG_BLOCK, 
			&stTempSigSet1, 
			&stTempSigSet2, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), SIGUSR2 );
	::kill( ::getpid(), SIGUSR1 );
	::kill( ::getpid(), SIGUSR1 );

	::fprintf( stderr, "[CDSignalMask End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalMask Start][L:%d]\n", __LINE__ );

	// stTempSigSet1(�ñ׳� ���� ���� ��ü) ���� �ñ׳��� ��������(SIG_UNBLOCK) �ϵ��� ����
	// ���� ������ ���� ������ stTempSigSet2(�ñ׳� ���� ���� ��ü) �ȿ� ����
	if( CDSignalMask( SIG_UNBLOCK, &stTempSigSet1, &stTempSigSet2 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[sigaddset( %d %d %d ) Error][E:%d][L:%d]\n", 
			SIG_BLOCK, 
			&stTempSigSet1, 
			&stTempSigSet2, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[CDSignalMask End][L:%d]\n", __LINE__ );

	::kill( ::getpid(), SIGUSR2 );
	::kill( ::getpid(), SIGUSR1 );

	::fprintf( stderr, "\n\n[CDSignalMask Start][L:%d]\n", __LINE__ );

	// ���� �������� �ñ׳� ���� ���� stTempSigSet2(�ñ׳� ���� ���� ��ü)��
	// �������� ����
	// ����° ����(NULL)�� NULL�� �ƴϰ� �ñ׳� ���� ���� ��ü�� �ּҸ� 
	// ���� �� ��� �ش� ��ü�� �������� ���� �ñ׳� ������ ����
	if( CDSignalMask( SIG_SETMASK, &stTempSigSet2, NULL ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[sigaddset( %d %d %d ) Error][E:%d][L:%d]\n", 
			SIG_BLOCK, 
			&stTempSigSet1, 
			&stTempSigSet2, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), SIGUSR2 );
	::kill( ::getpid(), SIGUSR1 );

	::fprintf( stderr, "[CDSignalMask End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalBlock Start][L:%d]\n", __LINE__ );

	// �������� Ư�� �ñ׳�(SIGUSR1)�� ����
	if( CDSignalBlock( SIGUSR1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalBlock( %d ) Error][E:%d][L:%d]\n", 
			SIGUSR1, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[CDSignalBlock End][L:%d]\n", __LINE__ );

	::kill( ::getpid(), SIGUSR1 );

	::fprintf( stderr, "\n\n[CDSignalUnBlock Start][L:%d]\n", __LINE__ );

	// �������� Ư�� �ñ׳�(SIGUSR1)�� ���� ����
	if( CDSignalUnBlock( SIGUSR1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalUnBlock( %d ) Error][E:%d][L:%d]\n", 
			SIGUSR1, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[CDSignalUnBlock End][L:%d]\n", __LINE__ );

	::kill( ::getpid(), SIGUSR1 );

	::fprintf( stderr, "\n\n[CDSignalAllBlock Start][L:%d]\n", __LINE__ );

	// �������� ��� �ñ׳��� ����
	// ���� �ñ׳� ���� ������ �ñ׳� ���� ���� ��ü(stTempSigSet1)�� ����
	if( CDSignalAllBlock( &stTempSigSet1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalAllBlock( %d ) Error][E:%d][L:%d]\n", 
			&stTempSigSet1, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[CDSignalAllBlock End][L:%d]\n", __LINE__ );

	::kill( ::getpid(), SIGUSR2 );

	::fprintf( stderr, "\n\n[CDSignalAllUnBlock Start][L:%d]\n", __LINE__ );

	// �������� ��� ���� �ñ׳� ���� ����
	if( CDSignalAllUnBlock() == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalAllUnBlock() Error][E:%d][L:%d]\n", 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[CDSignalAllUnBlock End][L:%d]\n", __LINE__ );

	::kill( ::getpid(), SIGUSR2 );

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	::sigemptyset( &stTempSigSet1 );
	::sigaddset( &stTempSigSet1, SIGUSR1 );
	::sigaddset( &stTempSigSet1, SIGUSR2 );
	::sigaddset( &stTempSigSet1, SIGALRM );
	::sigaddset( &stTempSigSet1, SIGRTMIN );

	::alarm( 10 );

	::fprintf( stderr, "\n\n[CDSignalWait Start][L:%d]\n", __LINE__ );

	// stTempSigSet1(�ñ׳� ���� ���� ��ü)�� ��� �� �ñ׳��� �߻��Ҷ� ���� ������ ���
	// �ñ׳��� �߻� �� ��� �ñ׳��� ������ �ñ׳� ���� ��ü(stSigInfo1)�� ����
	// stTempSigSet1�� ��ϵ� �ñ׳ε��� CDSignalWait �� ����Ǳ�����
	// ���ܵǾ� �־�� �Ѵ�
	if( ( iResult = CDSignalWait( &stTempSigSet1, &stSigInfo1 ) ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalWait( %d %d ) Error][E:%d][L:%d]\n", 
			&stTempSigSet1, 
			&stSigInfo1, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[CDSignalWait End][L:%d]\n", __LINE__ );

	RtsPrint( &stSigInfo1 );

	stTimeSpec.tv_sec	= 1;
	stTimeSpec.tv_nsec	= 0;

	::fprintf( stderr, "\n\n[CDSignalTimedWait Start][L:%d]\n", __LINE__ );

	::alarm( 10 );

	while( 1 )
	{
		// stTempSigSet1(�ñ׳� ���� ���� ��ü)�� ��� �� �ñ׳��� �߻��Ҷ� ������ �ð�(stTimeSpec)��ŭ ���
		// �ñ׳��� �߻� �� ��� �ñ׳��� ������ �ñ׳� ���� ��ü(stSigInfo1)�� ����
		// stTempSigSet1�� ��ϵ� �ñ׳ε��� CDSignalWait �� ����Ǳ�����
		// ���ܵǾ� �־�� �Ѵ�
		if( ( ( iResult = CDSignalTimedWait( &stTempSigSet1, &stSigInfo1, &stTimeSpec ) ) == -1 ) && errno == EAGAIN )
		{
			::fprintf( stderr, "[CDSignalTimedWait TimeOut][R:%d][E:%d][L:%d]\n", 
				iResult, 
				errno, 
				__LINE__ );

			continue;
		}

		::fprintf( stderr, "[Receive Alarm][Sig:%d][Code:%d][SigNo:%d][L:%d]\n", 
			iResult, 
			stSigInfo1.si_code, 
			stSigInfo1.si_signo, 
			__LINE__ );

		break;
	}

	::fprintf( stderr, "[CDSignalTimedWait End][L:%d]\n", __LINE__ );

	RtsPrint( &stSigInfo1 );

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	::fprintf( stderr, "\n\n[CDSignalWait Start][L:%d]\n", __LINE__ );

	// stTempSigSet1(�ñ׳� ���� ���� ��ü)�� ��� �� �ñ׳��� �߻��Ҷ� ���� ������ ���
	// �ñ׳��� �߻� �� ��� �ñ׳��� ������ �ñ׳� ���� ��ü(stSigInfo1)�� ����
	// stTempSigSet1�� ��ϵ� �ñ׳ε��� CDSignalWait �� ����Ǳ�����
	// ���ܵǾ� �־�� �Ѵ�
	if( ( iResult = CDSignalWait( &stTempSigSet1, &stSigInfo1 ) ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalWait( %d %d ) Error][E:%d][L:%d]\n", 
			&stTempSigSet1, 
			&stSigInfo1, 
			errno, 
			__LINE__ );

		exit( -1 );
	}

	::fprintf( stderr, "[CDSignalWait End][L:%d]\n", __LINE__ );

	RtsPrint( &stSigInfo1 );

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	::sleep( 1 );

	return 0;
}

// �Ϲ� �ñ׳� ó�� �Լ�
void SignalHandler1( int _iSignal )
{
	::fprintf( stderr, "[SignalHandler1][Signal Receive][Sig:%d][L:%d]\n", 
		_iSignal, __LINE__ );

	return;
}

// �Ϲ� �ñ׳� ó�� �Լ�
void SignalHandler2( int _iSignal )
{
	::fprintf( stderr, "[SignalHandler2][Signal Receive][Sig:%d][L:%d]\n", 
		_iSignal, __LINE__ );

	return;
}

// RTS(�ǽð� Ȯ�� �ñ׳�) ó�� �Լ�
void RtSignalHandler1( int _iSignal, siginfo_t* _stpSigInfo, void* _iSigVal )
{
	switch( _stpSigInfo->si_code )
	{
		case SI_USER:
			::fprintf( stderr, "[RtSignalHandler1][SI_USER Receive][Sig:%d][Code:%d][L:%d]\n", 
				_iSignal, _stpSigInfo->si_code, __LINE__ );

			break;

		case SI_QUEUE:
			::fprintf( stderr, "[RtSignalHandler1][SI_QUEUE Receive][Sig:%d][Code:%d][L:%d]\n", 
				_iSignal, _stpSigInfo->si_code, __LINE__ );

			break;

		default:
			break;
	}

	return;
}

// RTS(�ǽð� Ȯ�� �ñ׳�) ó�� �Լ�
void RtSignalHandler2( int _iSignal, siginfo_t* _stpSigInfo, void* _iSigVal )
{
	switch( _stpSigInfo->si_code )
	{
		case SI_USER:
			::fprintf( stderr, "[RtSignalHandler2][SI_USER Receive][Sig:%d][Code:%d][L:%d]\n", 
				_iSignal, _stpSigInfo->si_code, __LINE__ );

			break;

		case SI_QUEUE:
			::fprintf( stderr, "[RtSignalHandler2][SI_QUEUE Receive][Sig:%d][Code:%d][L:%d]\n", 
				_iSignal, _stpSigInfo->si_code, __LINE__ );

			break;

		default:
			break;
	}

	return;
}

// RTS(�ǽð� Ȯ�� �ñ׳�) ������ ��ü ȭ�� ��� �Լ�
void RtsPrint( siginfo_t* _stpSigInfo )
{
	switch( _stpSigInfo->si_code )
	{
		case SI_USER:
			::fprintf( stderr, "[RtsPrint][SI_USER Receive][Sig:%d][Code:%d][L:%d]\n", 
				_stpSigInfo->si_signo, 
				_stpSigInfo->si_code, 
				__LINE__ );

			break;

		case SI_QUEUE:
			::fprintf( stderr, "[RtsPrint][SI_QUEUE Receive][Sig:%d][Code:%d][L:%d]\n", 
				_stpSigInfo->si_signo, 
				_stpSigInfo->si_code, 
				__LINE__ );

			break;

		default:
			::fprintf( stderr, "[RtsPrint][ETC Receive][Sig:%d][Code:%d][L:%d]\n", 
				_stpSigInfo->si_signo, 
				_stpSigInfo->si_code, 
				__LINE__ );

			break;
	}

	return;
}