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

	// 시그널 차단 정보 객체
	sigset_t			stTempSigSet1;
	sigset_t			stTempSigSet2;

	// 시그널 정보 객체
	siginfo_t			stSigInfo1;
	siginfo_t			stSigInfo2;

	timespec			stTimeSpec;

	// 시그널 등록 객체
	struct sigaction	stTempSigAction;

	////////////////////////////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////////////////

	// 특정 함수(SignalHandler1)를 시그널 처리 함수로 등록
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

	// 특정 함수(SignalHandler2)를 특정 시그널(SIGUSR1) 처리 함수로 등록
	// 이전의 시그널 정보는 stTempSigAction(시그널 등록 객체)에 저장
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

	// 쓰레드가 특정 시그널(SIGUSR1)을 무시하도록 설정
	// 이전의 시그널 정보는 stTempSigAction(시그널 등록 객체)에 저장
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

	// 특정 함수(RtSignalHandler1)를 RTS(실시간 확장 시그널) 처리 함수로 등록
	if( CDSignalInitRts( RtSignalHandler1 ) == CD_SIGNAL_ERROR )
	{
		::fprintf( stderr, "[CDSignalInitRts( %d ) Error][E:%d][L:%d]\n", 
			RtSignalHandler1, errno, __LINE__ );

		exit( -1 );
	}

	::kill( ::getpid(), 15 );

	::fprintf( stderr, "[CDSignalInitRts() End][L:%d]\n", __LINE__ );

	::fprintf( stderr, "\n\n[CDSignalRtsAction() Start][L:%d]\n", __LINE__ );

	// 특정 시그널(SIGUSR2)을 RTS(실시간 확장 시그널) 처리 함수가
	// 처리 하도록 시그널 처리 함수(RtSignalHandler2)로 등록
	// 이전의 시그널 정보는 stTempSigAction(시그널 등록 객체)에 저장
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

	// 쓰레드가 특정 시그널(SIGUSR1)을 무시하도록 설정
	// 이전의 시그널 정보는 stTempSigAction(시그널 등록 객체)에 저장
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

	// 특정 함수(SignalHandler1)를 시그널 처리 함수로 등록
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

	// 시그널 차단 정보 객체(stTempSigSet1) 안의 데이터를
	// 모두 초기화(등록된 시그널이 없음)
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

	// 시그널 차단 정보 객체(stTempSigSet1)안에 특정 시그널(SIGUSR1) 추가
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

	// stTempSigSet1(시그널 차단 정보 객체) 안의 시그널을 차단(SIG_BLOCK) 하도록 설정
	// 기존 쓰레드 블록 정보는 stTempSigSet2(시그널 차단 정보 객체) 안에 저장
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

	// stTempSigSet1(시그널 차단 정보 객체) 안의 시그널을 차단해제(SIG_UNBLOCK) 하도록 설정
	// 기존 쓰레드 블록 정보는 stTempSigSet2(시그널 차단 정보 객체) 안에 저장
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

	// 현재 쓰레드의 시그널 차단 설정 stTempSigSet2(시그널 차단 정보 객체)의
	// 설정으로 변경
	// 세번째 인자(NULL)가 NULL이 아니고 시그널 차단 정보 객체의 주소를 
	// 전달 할 경우 해당 객체에 쓰레드의 기존 시그널 정보가 저장
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

	// 쓰레드의 특정 시그널(SIGUSR1)을 차단
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

	// 쓰레드의 특정 시그널(SIGUSR1)을 차단 해제
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

	// 쓰레드의 모든 시그널을 차단
	// 기존 시그널 차단 정보는 시그널 차단 정보 객체(stTempSigSet1)에 저장
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

	// 쓰레드의 모든 차단 시그널 차단 해제
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

	// stTempSigSet1(시그널 차단 정보 객체)에 등록 된 시그널이 발생할때 까지 무제한 대기
	// 시그널이 발생 할 경우 시그널의 정보는 시그널 정보 객체(stSigInfo1)에 저장
	// stTempSigSet1에 등록된 시그널들은 CDSignalWait 가 실행되기전에
	// 차단되어 있어야 한다
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
		// stTempSigSet1(시그널 차단 정보 객체)에 등록 된 시그널이 발생할때 정해진 시간(stTimeSpec)만큼 대기
		// 시그널이 발생 할 경우 시그널의 정보는 시그널 정보 객체(stSigInfo1)에 저장
		// stTempSigSet1에 등록된 시그널들은 CDSignalWait 가 실행되기전에
		// 차단되어 있어야 한다
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

	// stTempSigSet1(시그널 차단 정보 객체)에 등록 된 시그널이 발생할때 까지 무제한 대기
	// 시그널이 발생 할 경우 시그널의 정보는 시그널 정보 객체(stSigInfo1)에 저장
	// stTempSigSet1에 등록된 시그널들은 CDSignalWait 가 실행되기전에
	// 차단되어 있어야 한다
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

// 일반 시그널 처리 함수
void SignalHandler1( int _iSignal )
{
	::fprintf( stderr, "[SignalHandler1][Signal Receive][Sig:%d][L:%d]\n", 
		_iSignal, __LINE__ );

	return;
}

// 일반 시그널 처리 함수
void SignalHandler2( int _iSignal )
{
	::fprintf( stderr, "[SignalHandler2][Signal Receive][Sig:%d][L:%d]\n", 
		_iSignal, __LINE__ );

	return;
}

// RTS(실시간 확장 시그널) 처리 함수
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

// RTS(실시간 확장 시그널) 처리 함수
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

// RTS(실시간 확장 시그널) 데이터 객체 화면 출력 함수
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
