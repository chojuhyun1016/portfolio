#!/bin/sh

OS=`uname -r`

if [ $OS = "5.10" ]; then
	#echo "A"
	make -f Makefile_510
elif [ $OS = "5.8" ]; then
	#echo "B"
	make -f Makefile_508
else
	echo "unknown OS"
fi
