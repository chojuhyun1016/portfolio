#include "CNLinkedList.h"

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

// ----------------------------------------------------
// Linked List �ʱ�ȭ
// ----------------------------------------------------
void CNInitList( stNode* _stpNode )
{
	memset( _stpNode,0,sizeof( stNode ) );
	
	_stpNode->m_head.next = &_stpNode->m_tail;
	_stpNode->m_tail.prev = &_stpNode->m_head;
}


// ----------------------------------------------------
// �ش�ť(����Ʈ����)�� ��� �����Լ� 
// ----------------------------------------------------
stContainer* CNPushList( stNode* _stpNode, char* _cpPut, int _iPutSize, int _iSequence )
{
	stContainer		*dummy	=	NULL;

	if( _iPutSize > MAX_CONTAINER_DATA_SIZE )
		return	NULL;

	if( !( dummy = ( stContainer* )malloc( sizeof( stContainer ) ) ) )
		return NULL;

	memset( dummy, 0x00, sizeof( stContainer ) );

	dummy->iSequence	= _iSequence;
	memcpy( dummy->data, _cpPut, _iPutSize ); 

	dummy->next = &_stpNode->m_tail;
	dummy->prev = _stpNode->m_tail.prev;
	dummy->prev->next = dummy;
	_stpNode->m_tail.prev = dummy;

	_stpNode->m_reference++;

	return dummy;
}


// ----------------------------------------------------
// �ش�ť(����Ʈ����)�κ��� �ش��带 �����ϴ� �Լ�
// ----------------------------------------------------
stContainer* CNDeleteList( stNode* _stpNode, stContainer* _stDelNode )
{
	stContainer *prev;
	
	_stDelNode->prev->next = _stDelNode->next;
	_stDelNode->next->prev = _stDelNode->prev;
	prev = _stDelNode->prev;

	free( _stDelNode );

	_stpNode->m_reference--;

	return prev;
}


// ----------------------------------------------------
// Ư�� SEQUENCE(UNIQUE) ���� ������ ������ �����̳� �˻�
// ----------------------------------------------------
stContainer* CNSerachData( stNode* stpNode, int _iSequence )
{
	stContainer *dummy = NULL;
	
	for( dummy = stpNode->m_head.next; dummy->next; dummy = dummy->next )
	{
		if( dummy->iSequence == _iSequence )
			return	dummy;
	}
	
	return	NULL;
}


// ----------------------------------------------------
// �ش�ť(����Ʈ����)�κ��� ����带 �����ϴ� �Լ�
// ----------------------------------------------------
void CNDeleteAllList( stNode* _stpNode )
{
	stContainer *stpData = NULL;

	if( _stpNode->m_reference <= 0 )
		return;

	for( stpData = _stpNode->m_head.next; stpData->next; stpData = stpData->next )
		stpData = CNDeleteList( _stpNode, stpData );

	return;
}


// ----------------------------------------------------
// ����Ʈ���� ��� �����͸� ����ϴ� �Լ�
// ----------------------------------------------------
void CNDisplayList( stNode* stpNode )
{
	stContainer *dummy = NULL;
	
	if( stpNode->m_head.next == NULL )
		return;

	for( dummy = stpNode->m_head.next; dummy->next; dummy = dummy->next )
	{
		//printf("PRINT XML : [%s]\n", dummy->data );
	}
	
	return;
}


// ----------------------------------------------------
// ���� �� ����Ʈ�� ī��Ʈ�� ��ȯ�ϴ� �Լ�
// ----------------------------------------------------
int CNGetListCnt( stNode* _stpNode )
{
	int iCount			= 0;
	stContainer *data	= NULL;

	for( data = _stpNode->m_head.next; data->next; data=data->next )
		iCount++;
	
	return iCount;
}
