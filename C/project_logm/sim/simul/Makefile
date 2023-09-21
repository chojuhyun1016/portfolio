HELL = /bin/sh
CXX=        /opt/SUNWspro/bin/CC

DEFS		= -g

SVCBASE		= /data4/rtcdrp/sms
BINDIR		= /data4/rtcdrp/bin

KTFINC      = -I. -I../
KTFLIB      = -L/export/home/sms4/LOGM
LIBLINK     = -lloggerwriter

CCOPTIONS	= $(KTFINC) -compat=4 -c $(DEFS) -D_POSIX_PTHREAD_SEMANTICS
LLOPTIONS	= $(KTFLIB) -O -compat=4

OBJS = Test.o

all: TEST

TEST: $(OBJS)
		$(CXX) -o TEST $(OBJS) $(LLOPTIONS) $(LIBLINK)

Test.o: Test.cpp 
		$(CXX) -O $(CCOPTIONS) Test.cpp

clean:
		rm -f ./Test.o
		rm -f ./TEST
