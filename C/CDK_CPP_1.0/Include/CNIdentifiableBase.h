#ifndef	__CNOBJECT_IDENTIFIABLE_BASE__
#define	__CNOBJECT_IDENTIFIABLE_BASE__

#include "CNDefinitions.h"

//-----------------------------------------------------------------------------
//
// ICNIdentifiable
//
// 1. ICNIdentifiable��?
//     �̸��� �����ϰ� ��� Interface Ŭ����
//
// 2. ICNIdentifiable�� ���ۼ���!!
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
	// 1) Pool Name��  �����ϰ� ��� �Լ�.
	virtual	const char*			GetName() const PURE;
	virtual	void				SetName( const char* _cpName ) PURE;
};

}

#endif
