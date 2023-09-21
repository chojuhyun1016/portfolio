#include "CNCommonUtil.h"

#include <strings.h>
#include <time.h>
#include <sys/time.h>


float CNCommonUtil::CNQueryPerformance::GetGapTime() const
{
    long	sec;
    long	usec;

    sec = m_timeNow.tv_sec - m_timePre.tv_sec;
    usec = m_timeNow.tv_usec - m_timePre.tv_usec;

    if ( usec < 0 )
    {
        sec -= 1;
        usec += 1000000;
    }

	return	(float)sec + (float)usec / 1000000;
}

void CNCommonUtil::CNTrace::Trace( const char *fmt,... )
{
	// Check) fmt가 NULL이면 안된다.
	RETURN_IF( fmt == NULL, );

	char		sFileBuffer[256];
	char		sTempBuffer[2048];

	FILE*		fp;

	timeval		tvTime;
	struct	tm	stTm; 

	va_list		ap;

	static		pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;

	// 1) valist 시작
	//va_start( ap, fmt );

	// 2) 시간 셋팅

	// !!! thread unsafe !!! Async-Signal-Safe
	gettimeofday( &tvTime, NULL );

	localtime_r( &tvTime.tv_sec, &stTm );

	// 3) File Name 셋팅
	// !!! thread unsafe !!! Async-Signal-Safe
	snprintf( sFileBuffer, sizeof( sFileBuffer ), "TRACE.%04d%02d%02d", stTm.tm_year + 1900, stTm.tm_mon + 1, stTm.tm_mday );
	
	// 4) 로그데이터 셋팅
	// !!! thread unsafe !!! Async-Signal-Safe
	snprintf( sTempBuffer, sizeof( sTempBuffer ), "[%02d:%02d:%02d]", stTm.tm_hour, stTm.tm_min, stTm.tm_sec );
	
	// !!! thread unsafe !!! Async-Signal-Safe
	va_start( ap, fmt );	
	// !!! thread unsafe !!! Async-Signal-Safe
	vsnprintf( sTempBuffer + 10, sizeof( sTempBuffer ), fmt, ap );
	// !!! thread unsafe !!! Async-Signal-Safe
	va_end( ap );

	RETURN_IF( ( fp = fopen( sFileBuffer, "a+" ) ) == NULL, );

	// 5) 출력한다.

	{
		MutexLock MLock( &mutex );

		fwrite( sTempBuffer, strlen( sTempBuffer ), 1, fp );

		fflush( fp );
	}

	fclose( fp );
}


void CNCommonUtil::CNTimeSleep::MilliSleep( unsigned long _iMilli )
{
	struct	timeval	tv;
	
	if( _iMilli >= 1000000 )
	{
		tv.tv_sec = _iMilli / 1000000;
		tv.tv_usec = _iMilli % 1000000;
	}
	else
	{
		tv.tv_sec = 0;
		tv.tv_usec = _iMilli;
	}

	select( 0, NULL, NULL, NULL, &tv );

	return;
}


void CNCommonUtil::CNTimeSleep::NanoSleep( unsigned long _iNano )
{
	struct timespec	ts;

	// Overflow 예외 처리
	if( _iNano >= 4000000000 )
		_iNano = 4000000000;

	if( _iNano >= 1000000000 )
	{
		ts.tv_sec = _iNano / 1000000000;
		ts.tv_nsec = _iNano % 1000000000;
	}
	else
	{
		ts.tv_sec = 0;
		ts.tv_nsec = _iNano;
	}	

	nanosleep( &ts, NULL );
}

