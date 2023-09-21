#ifndef __CN_UTIL_MUTEX_LOCK__
#define __CN_UTIL_MUTEX_LOCK__

#include <stdio.h>
#include <unistd.h>  
#include <pthread.h>  

#include "CND.h"
#include "CNDefinitions.h"

namespace CNCommonUtil
{

class MutexLock
{
	public:
		MutexLock( pthread_mutex_t* _mutex ) : mutex(_mutex) {  if( pthread_mutex_lock( mutex ) ) { fprintf( stderr, "pthread_mutex_lock() Error\n" ); } }
		~MutexLock() { if( pthread_mutex_unlock( mutex ) ) { fprintf( stderr, "pthread_mutex_unlock() Error\n" ); } }

	private:
		MutexLock( const MutexLock& mutex ) EMPTY
		MutexLock& operator=( const MutexLock& mutex );

	private:
		pthread_mutex_t* mutex;
};

}

#endif

