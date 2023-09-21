CC = g++

DEFS	= -g
#DEFS	+= -DDEBUG

INCLUDES 	= -I. -I../../../Inc

LDIRS 		= -L. -L../../../Bin

CFLAGS = $(DEFS) -D_POSIX_THREADS -D_POSIX_PTHREAD_SEMANTICS -D_REENTRANT

LIBS = -lsocket -lnsl -ldl -lrt -lCDBase

all: CD_BASE_TUTORIAL

CD_BASE_TUTORIAL:	Main.o
	$(CC) -o $@ $(CFLAGS) Main.o $(INCLUDES) $(LDIRS) $(LIBS)

Main.o: Main.c
	$(CC) -c Main.c $(CFLAGS) $(INCLUDES) $(LDIRS) $(LIBS)

clean:
	rm -f ./*.o
	rm -f ./CD_BASE_TUTORIAL
	rm -f ./core

