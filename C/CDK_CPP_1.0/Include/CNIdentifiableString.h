#ifndef	__CNOBJECT_IDENTIFIABLE_STRING__
#define	__CNOBJECT_IDENTIFIABLE_STRING__

#include "CNDefinitions.h"
#include "CNIdentifiableBase.h"

//-----------------------------------------------------------------------------
//
// ICNIdentifiable
//
// 1. ICNIdentifiable란?
//     이름을 가지고 있는 
//    의미 한다.
//     일반적으로 Factory는 원하는 종류의 객체를 생산하여 할당하고 일련번호를
//    붙혀 관리한다. 또 다 사용한 객체를 돌려받으면 그 객체를 관리하고 있다가
//    다시 객체의 할당을 원할 때 할당해주는 Pool의 역할도 한다.
//
// 2. Factory의 동작설명!!
//    1) Product(생산)
//    2) Disuse(폐기)
// 
//    주의) ProductSeiral과 Shipment Serial에 대해서는 CProductable에서 확인...
//
//-----------------------------------------------------------------------------
namespace CNIdentifiable
{

template <int ISIZE=256>
class NString : 
// ****************************************************************************
// Inherited Classes)
// ----------------------------------------------------------------------------
	virtual public				CNIdentifiable::IBase
{
	// ****************************************************************************
	// Constructor/Destructor)
	// ----------------------------------------------------------------------------
	public:
		NString( const char* _cpName = NULL )						{ SetName( _cpName ); }
		~NString()													{}

	// ****************************************************************************
	// Publics)
	// ----------------------------------------------------------------------------
	public:
		// 1) Pool Name을  설정하고 얻는 함수.
		virtual	const char*			GetName() const							{ return m_strName; }
		virtual	void				SetName(const char* _cpName)			{ if( _cpName != 0) strlcpy( m_strName, _cpName, ISIZE ); else m_strName[0] = 0; }

	// ****************************************************************************
	// Implementations)
	// ----------------------------------------------------------------------------
	private:
		// 1) Pool Name
		char				m_strName[ISIZE];
};

}

#endif

