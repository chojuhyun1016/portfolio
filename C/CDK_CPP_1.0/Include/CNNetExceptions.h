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
	// Base) Receive�� �������� ��� �߻��Ǵ� Exception
	// ------------------------------------------------------------------------
	// Exceptions) Invalid Socket�� �������� ��� �߻���Ű�� Exception
	// ------------------------------------------------------------------------
	class InvalidSocket : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Invalid Socket"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Bind�� �������� ��� �߻���Ű�� Exception
	// ------------------------------------------------------------------------
	class FailToBind : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to bind"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Accept�� �������� ��� �߻���Ű�� Exception
	// ------------------------------------------------------------------------
	class FailToAccept : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to Accept"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Receive�� �������� ��� �߻���Ű�� Exception
	// ------------------------------------------------------------------------
	class FailToReceive : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to Receive"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Socket������ �������� ��� �߻���Ű�� Exception
	// ------------------------------------------------------------------------
	class FailToCreateSocket : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to create socket"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Socket������ �������� ��� �߻���Ű�� Exception
	// ------------------------------------------------------------------------
	class FailToSetSocketOption : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Fail to set socket option"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Packet Size�� �ʹ� ���� ��� �߻���Ű�� Exception�Դϴ�. 
	//             TCP�� ��� SizeOfPacket���� ���� ��� �߻��մϴ�.
	// ------------------------------------------------------------------------
	class TooShortPacketSize : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Packet Size is too short"; }
	};

	// ------------------------------------------------------------------------
	// Exceptions) Packet Size�� �ʹ� Ŭ ��� �߻���Ű�� Exception�Դϴ�. 
	//             TCP�� ��� SizeOfPacket���� ���� ��� �߻��մϴ�.
	// ------------------------------------------------------------------------
	class TooLongPacketSize : public std::exception
	{
		public:
			virtual const char* what() const throw() { return	"Packet Size is too long"; }
	};
}

}

#endif
