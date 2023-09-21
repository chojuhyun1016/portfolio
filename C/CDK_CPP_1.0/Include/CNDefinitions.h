//*****************************************************************************
//*                                                                           *
//*                               Definitions                                 *
//*                                                                           *
//*                Definition for Global use of Application                   *
//*                                                                           *
//*****************************************************************************

#ifndef __CN_DEFINITION__
#define __CN_DEFINITION__

// ----------------------------------------------------------------------------
// include
// ----------------------------------------------------------------------------
#include <assert.h>

//*****************************************************************************
//                  Cho Juhyun's Network Basic Definitions                    *
//*****************************************************************************
#ifndef BOOL
typedef int								BOOL;
#endif

#ifndef HANDLE
typedef void*							HANDLE;
#endif

#ifndef INVALID_HANDLE_VALUE
#define INVALID_HANDLE_VALUE			0
#endif

#ifndef FALSE
#define FALSE							0
#endif

#ifndef TRUE
#define TRUE							1
#endif

#ifndef PTHREAD_SUCCESS
#define PTHREAD_SUCCESS					0
#endif

#ifndef PTHREAD_ERROR
#define PTHREAD_ERROR					-1
#endif

#ifndef INLINE
#define	INLINE							inline
#endif

#ifndef PURE
#define	PURE							= 0
#endif

#ifndef EMPTY
#define	EMPTY							{}
#endif

#ifndef EMPTY_TRUE
#define	EMPTY_TRUE						{ return TRUE; }
#endif

#ifndef EMPTY_FALSE
#define	EMPTY_FALSE						{ return FALSE; }
#endif

#ifndef EMPTY_ZERO
#define	EMPTY_ZERO						{ return 0; }
#endif


#ifndef SAFE_FREE
#define	SAFE_FREE(p)					if( p ){ free( p ); p = NULL; }
#endif

#ifndef SAFE_DELETE
#define	SAFE_DELETE(p)					if( p ){ delete p; p = NULL; }
#endif

#ifndef SAFE_DELETE_ARRAY
#define	SAFE_DELETE_ARRAY(p)			if( p ){ delete []p; p=NULL; }
#endif


#ifndef SAFE_DELETE_ARRAY
#define SAFE_DELETE_ARRAY(p)			{ if( p ){ delete[](p);   (p) = NULL; } }
#endif

#ifndef SAFE_RELEASE
#define	SAFE_RELEASE(data)				if( data ){ (data)->Release(); (data) = NULL; }
#endif

#ifndef SAFE_CLOSEHANDLE
#define	SAFE_CLOSEHANDLE(data)			if( data != INVALID_HANDLE_VALUE ){ CloseHandle( data ); data = INVALID_HANDLE_VALUE; }
#endif


// ----------------------------------------------------------------------------
// assert 관련
// ----------------------------------------------------------------------------
#define ASSERT( f )										assert( f )

#ifdef DEBUG
#define	USE_CNNET_ASSERT
#endif

#ifdef USE_CNNET_ASSERT
	#define	CNASSERT( equation, value )					ASSERT( equation );	if( !(equation) ){ return value; }
	#define	CNASSERT_ERROR( equation)					ASSERT( equation );
	#define	CNASSERT_ERROR_CRITICAL						ASSERT( FALSE );
	#define	CNASSERT_ERROR_TRACE( equation, state )		ASSERT( equation );	if( !(equation) ){ state; }
#else
	#define	CNASSERT( equation, value )
	#define	CNASSERT_ERROR( equation )
	#define	CNASSERT_ERROR_CRITICAL
	#define	CJASSERT_ERROR_TRACE( equation, string )
#endif

// ----------------------------------------------------------------------------
// Trace 관련
// ----------------------------------------------------------------------------
#ifdef DEBUG
#define	USE_CNNET_TRACE
#endif

#ifndef TRACE
#define TRACE											CNCommonUtil::CNTrace::Trace
#endif

#ifndef ERROR_TRACE
#define ERROR_TRACE										TRACE
#endif

#ifdef USE_CNNET_TRACE
	#ifndef INFO_TRACE
	#define INFO_TRACE									TRACE
	#endif
#else
	#ifndef INFO_TRACE
	#define INFO_TRACE									TRACE
	#endif
#endif

// ----------------------------------------------------------------------------
// Check 관련
// ----------------------------------------------------------------------------
// 1) 제어문 Serise
#define	CONTINUE_IF( equation )											if( equation)			{ continue; }
#define	BREAK_IF( equation )											if( equation)			{ break; }
#define	THROW_IF( equation, value )										if( equation)			{ throw value; }
#define	RETURN_IF( equation, value )									if( equation)			{ return value; }

#define	SAFE_DELETE_AND_CND_RETURN_IF( condition, object, value )		if( condition)			{ SAFE_DELETE(object); return value; }
#define	DELETE_AND_CND_RETURN_IF( condition, object, value )			if( condition)			{ delete object; return value; }
#define	DELETE_SELF_AND_CND_RETURN_IF( condition, value )				if( condition)			{ delete this; return value; }

#define	GOTO_IF( equation, destination )								if( equation)			{ goto destination; }

// 2) Error Serise
#define	ERROR_CONTINUE_IF( equation, state )							if( equation)			{ state; continue; }
#define	ERROR_BREAK_IF( equation, state )								if( equation)			{ state; break; }
#define	ERROR_THROW( value, state )																{ state; throw value; }
#define	ERROR_THROW_IF( equation, value, state )						if( equation)			{ state; throw value; }
#define	ERROR_RETURN_IF( equation, value, state )						if( equation)			{ state; return value; }

// 3) 제어문 Serise
#define	ERROR_IF( equation, state )										if( equation)			{ state; }

// 4) dynamic bind Check
#define	HAS_ATTRIBUTE( att, ptr )										( dynamic_cast<att*>(ptr) != NULL )

// 1) Set 관련
#define	SET_ZERO( variable )											variable = 0
#define SET_ZERO_VECTOR3( variable )									variable.x = variable.y = variable.z = 0.0f;
#define SET_ZERO_VECTOR4( variable )									variable.x = variable.y = variable.z = variable.w = 0.0f;

#define	SET_NULL( variable )											variable = NULL
#define	SET_TRUE( variable )											variable = TRUE
#define	SET_FALSE( variable )											variable = FALSE

#endif



