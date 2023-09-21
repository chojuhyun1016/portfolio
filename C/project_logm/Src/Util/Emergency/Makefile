# SHELL = /bin/sh
CXX=        /opt/SUNWspro/bin/CC

DEFS		= -g
DEFS		+= -mt
#DEFS		+= -DOS510
#DEFS		+= -DDEBUG

BINDIR		= /data4/rtcdrp/bin

INC_DIR		= -I. -I../../../Lib/CNDK_C/Inc
LIB_DIR		= -L. -L../../../Lib/CNDK_C/Bin
LIB_LINK	=  -lCNBase -lCNConfig -lCNIpc -lCNLog -lCNSignal -lCNSocket -lCNThread
LIBS = -lsocket -lnsl -ldl -lrt -lpthread

CCOPTIONS	= $(DEFS) -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

TARGET1 = SET_EMERGENCY
TARGET2 = GET_EMERGENCY

OBJETS1 = SetEmergency.o
OBJETS2 = GetEmergency.o

all: $(TARGET1) $(TARGET2)

SET_EMERGENCY: $(OBJETS1)
		$(CXX) -o $@ $(CCOPTIONS) $(OBJETS1) $(INC_DIR) $(LIB_DIR) $(LIB_LINK) $(LIBS)

GET_EMERGENCY: $(OBJETS2)
		$(CXX) -o $@ $(CCOPTIONS) $(OBJETS2) $(INC_DIR) $(LIB_DIR) $(LIB_LINK) $(LIBS)

SetEmergency.o: SetEmergency.c
		$(CXX) -c $*.c $(CCOPTIONS) $(INC_DIR) $(LIB_DIR) $(LIB_LINK) $(LIBS)

GetEmergency.o: GetEmergency.c
		$(CXX) -c $*.c $(CCOPTIONS) $(INC_DIR) $(LIB_DIR) $(LIB_LINK) $(LIBS)

clean:
		rm -f ./*.o
		rm -f ./$(TARGET1) ./$(TARGET2)
		rm -f ./core

