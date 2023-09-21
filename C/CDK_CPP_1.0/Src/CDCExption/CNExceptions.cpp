#include "CNExceptions.h"

CNException::IException::IException( const char *const& _What )
{
	if (_What != NULL )
	{
		size_t _Buf_size = strlen( _What ) + 1;
		m_what = static_cast<char*>( malloc( _Buf_size ) );

		if ( m_what != NULL )
		{
			strncpy( const_cast<char*>(m_what), _What, _Buf_size );
		}
	}
	else
	{
		m_what = NULL;
	}
	m_doFree = 1;
}

CNException::IException::IException( const char *const& _What, int )
{
	m_what		= _What;
	m_doFree	= 0;
}

CNException::IException::IException( const IException& _That )
{
	m_doFree = _That.m_doFree;

	if( m_doFree )
	{
		if( _That.m_what != NULL )
		{
			size_t _Buf_size = strlen( _That.m_what ) + 1;
			m_what = static_cast<char*>( malloc( _Buf_size ) );

			if( m_what != NULL )
				strncpy( const_cast<char*>(m_what), _That.m_what, _Buf_size );
		}
		else
		{
			m_what = NULL;
		}
	}
	else
		m_what = _That.m_what;
}

CNException::IException& CNException::IException::operator=( const IException& _That )
{
	if( this != &_That )
	{
		m_doFree = _That.m_doFree;
 
		if( m_doFree )
		{
			if( _That.m_what != NULL )
			{
				size_t _Buf_size = strlen( _That.m_what ) + 1;
				m_what = static_cast<char*>( malloc( _Buf_size ) );

				if( m_what != NULL )
					strncpy( const_cast<char*>(m_what), _That.m_what, _Buf_size );
			}
			else
				m_what = NULL;
		}
		else
			m_what = _That.m_what;
	}

	return	*this;
}

CNException::IException::~IException() throw()
{
	if( m_doFree )
		free( const_cast<char*>( m_what ) );
}

const char* CNException::IException::what() const throw()
{
	if( m_what != NULL )
		return m_what;
	else
		return "Unknown exception";
}