CC = /opt/SUNWspro/bin/CC

DEFS	= -g
#DEFS	+= -DDEBUG

INCLUDES 	= -I. -I../../Inc

LDIRS 		= -L. -L../../Bin

CFLAGS = $(DEFS) -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

LIBS = -lsocket -lnsl -ldl -lrt -lCDLog

all: CD_LOG_TUTORIAL

CD_LOG_TUTORIAL:	Main.o
	$(CC) -o $@ $(CFLAGS) Main.o $(INCLUDES) $(LDIRS) $(LIBS)

Main.o: Main.c
	$(CC) -c Main.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

rm:
	rm -f *.log

clean:
	rm -f ./*.o
	rm -f ./*.log
	rm -f ./CD_LOG_TUTORIAL
	rm -f ./core

