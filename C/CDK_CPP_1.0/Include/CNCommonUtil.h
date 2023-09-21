#ifndef __CN_UTIL_QUERY_PERFORMANCE__
#define __CN_UTIL_QUERY_PERFORMANCE__

#include <stdio.h>
#include <stdarg.h>
#include <sys/time.h>

#include "CND.h"
#include "CNDefinitions.h"
#include "CNMutexLock.h"

namespace CNCommonUtil
{


class CNQueryPerformance
{
	public:
		CNQueryPerformance()	EMPTY
		~CNQueryPerformance()	EMPTY

		float					GetGapTime() const;
		void					CheckTimeStart()		{ gettimeofday( &m_timePre, NULL ); }
		void					CheckTimeEnd()			{ gettimeofday( &m_timeNow, NULL ); }

	protected:
			
		struct timeval			m_timePre;
		struct timeval			m_timeNow;

};


class CNTrace
{
	public:

		CNTrace();
		~CNTrace();

		static void Trace( const char *fmt,... );
};


class CNTimeSleep
{
	public:

		CNTimeSleep();
		~CNTimeSleep();

		static void MilliSleep( unsigned long _iMilli );
		static void NanoSleep( unsigned long _iNano );
};


}

#endif

