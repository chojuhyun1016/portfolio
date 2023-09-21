# SHELL = /bin/sh
CXX=        /opt/SUNWspro/bin/CC

DEFS		= -g
DEFS		+= -mt
DEFS		+= -DOS510
#DEFS		+= -DDEBUG

BINDIR		= /data4/rtcdrp/bin

INC_DIR		= -I. -I../../../Lib/CNDK_C/Inc
LIB_DIR		= -L. -L../../../Lib/CNDK_C/Bin
LIB_LINK	=  -lCNBase -lCNConfig -lCNIpc -lCNLog -lCNSignal -lCNSocket -lCNThread
LIBS = -lsocket -lnsl -ldl -lrt -lpthread

CCOPTIONS	= $(DEFS) -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

TARGET = LOG_MANAGER

OBJETS = LogManager.o CNLinkedList.o

all: $(TARGET)

LOG_MANAGER: $(OBJETS)
		$(CXX) -o $@ $(CCOPTIONS) $(OBJETS) $(INC_DIR) $(LIB_DIR) $(LIB_LINK) $(LIBS)

LogManager.o: LogManager.c
		$(CXX) -c $*.c $(CCOPTIONS) $(INC_DIR) $(LIB_DIR) $(LIB_LINK) $(LIBS)

CNLinkedList.o: CNLinkedList.c
		$(CXX) -c $*.c $(CCOPTIONS) $(INC_DIR) $(LIB_DIR) $(LIB_LINK) $(LIBS)

clean:
		rm -f ./*.o
		rm -f ./$(TARGET)
		rm -f ./core

