#ifndef	__CNOBJECT_IDENTIFIABLE_STRING__
#define	__CNOBJECT_IDENTIFIABLE_STRING__

#include "CNDefinitions.h"
#include "CNIdentifiableBase.h"

//-----------------------------------------------------------------------------
//
// ICNIdentifiable
//
// 1. ICNIdentifiable��?
//     �̸��� ������ �ִ� 
//    �ǹ� �Ѵ�.
//     �Ϲ������� Factory�� ���ϴ� ������ ��ü�� �����Ͽ� �Ҵ��ϰ� �Ϸù�ȣ��
//    ���� �����Ѵ�. �� �� ����� ��ü�� ���������� �� ��ü�� �����ϰ� �ִٰ�
//    �ٽ� ��ü�� �Ҵ��� ���� �� �Ҵ����ִ� Pool�� ���ҵ� �Ѵ�.
//
// 2. Factory�� ���ۼ���!!
//    1) Product(����)
//    2) Disuse(���)
// 
//    ����) ProductSeiral�� Shipment Serial�� ���ؼ��� CProductable���� Ȯ��...
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
		// 1) Pool Name��  �����ϰ� ��� �Լ�.
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
