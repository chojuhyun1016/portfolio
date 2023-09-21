#ifndef	__CNOBJECT_IDENTIFIABLE_BASE__
#define	__CNOBJECT_IDENTIFIABLE_BASE__

#include "CNDefinitions.h"

//-----------------------------------------------------------------------------
//
// ICNIdentifiable
//
// 1. ICNIdentifiable란?
//     이름을 설정하고 얻는 Interface 클래스
//
// 2. ICNIdentifiable의 동작설명!!
//    1) GetName()
//    2) SetName(...)
// 
//
//-----------------------------------------------------------------------------
namespace CNIdentifiable
{

class IBase
{
// ****************************************************************************
// Publics)
// ----------------------------------------------------------------------------
public:
	// 1) Pool Name을  설정하고 얻는 함수.
	virtual	const char*			GetName() const PURE;
	virtual	void				SetName( const char* _cpName ) PURE;
};

}

#endif

