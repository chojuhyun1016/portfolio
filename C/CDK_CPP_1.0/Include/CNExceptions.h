#ifndef	__CN_IEXCEPTIONS__
#define	__CN_IEXCEPTIONS__

#include <exception>

#include "CNDefinitions.h"
#include "CNNetDefinitions.h"
//-----------------------------------------------------------------------------
//
// CNException::IThrowable
//
// 1. CNException::IThrowable��~
//     Exceptionó���� ���� Class��.
//
//
//-----------------------------------------------------------------------------

namespace CNException
{

// ----------------------------------------------------------------------------
//
//  Exception Throwable (std::exception�� ��ӹ���.)
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
		// 1) Send�� �� �θ��� �Լ�.
		virtual	void				OnException( const std::exception& _rException ) throw() EMPTY
};

}

#endif
