#ifndef	__CNNET_IO_EXCEPTIONS__
#define	__CNNET_IO_EXCEPTIONS__

#include <exception>
#include <CNExceptions.h>

#include "CNDefinitions.h"
#include "CNNetDefinitions.h"

namespace CNException
{
namespace NetIO
{
	// ----------------------------------------------------------------------------
	//
	//  Net I/O Exception
	//
	// ----------------------------------------------------------------------------
	// Base) Receive에 실패했을 경우 발생되는 Exception
	// ------------------------------------------------------------------------
	// Exceptions) Invalid Socket에 실패했을 경우 발생시키는 Exception
	// ------------------------------------------------------------------------
	class InvalidSocket : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Invalid Socket"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Bind에 실패했을 경우 발생시키는 Exception
	// ------------------------------------------------------------------------
	class FailToBind : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to bind"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Accept에 실패했을 경우 발생시키는 Exception
	// ------------------------------------------------------------------------
	class FailToAccept : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to Accept"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Receive에 실패했을 경우 발생시키는 Exception
	// ------------------------------------------------------------------------
	class FailToReceive : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to Receive"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Socket생성에 실패했을 경우 발생시키는 Exception
	// ------------------------------------------------------------------------
	class FailToCreateSocket : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to create socket"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Socket생성에 실패했을 경우 발생시키는 Exception
	// ------------------------------------------------------------------------
	class FailToSetSocketOption : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to set socket option"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Packet Size가 너무 작을 경우 발생시키는 Exception입니다. 
	//             TCP의 경우 SizeOfPacket보다 작을 경우 발생합니다.
	// ------------------------------------------------------------------------
	class TooShortPacketSize : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Packet Size is too short"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Packet Size가 너무 클 경우 발생시키는 Exception입니다. 
	//             TCP의 경우 SizeOfPacket보다 작을 경우 발생합니다.
	// ------------------------------------------------------------------------
	class TooLongPacketSize : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Packet Size is too long"; }
	};
}

}

#endif

