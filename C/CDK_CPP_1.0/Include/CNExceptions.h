#ifndef	__CN_IEXCEPTIONS__
#define	__CN_IEXCEPTIONS__

#include <exception>

#include "CNDefinitions.h"
#include "CNNetDefinitions.h"
//-----------------------------------------------------------------------------
//
// CNException::IThrowable
//
// 1. CNException::IThrowable는~
//     Exception처리를 위한 Class임.
//
//
//-----------------------------------------------------------------------------

namespace CNException
{

// ----------------------------------------------------------------------------
//
//  Exception Throwable (std::exception를 상속받음.)
//
// ----------------------------------------------------------------------------
class IThrowable : public std::exception
{
	public:
		IThrowable()														{}

	public:
		virtual	void	Description() EMPTY
};


// ----------------------------------------------------------------------------
//
//  Exception Handleable.
//
// ----------------------------------------------------------------------------
class IHandleable
{
	public:
		// 1) Send할 때 부르는 함수.
		virtual	void				OnException( const std::exception& _rException ) throw() EMPTY
};

}

#endif

