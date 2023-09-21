#!/bin/sh

echo ""
echo ""
echo "--- cd CDBase ---"

cd ./Src/CDBase
make clean
make


echo ""
echo ""
echo "--- cd CDConfig ---"

cd ../CDConfig
make clean
make


echo ""
echo ""
echo "--- cd CDIpc ---"

cd ../CDIpc
make clean
make


echo ""
echo ""
echo "--- cd CDLog ---"

cd ../CDLog
make clean
make


echo ""
echo ""
echo "--- cd CDSignal ---"

cd ../CDSignal
make clean
make


echo ""
echo ""
echo "--- cd CDSocket ---"

cd ../CDSocket
make clean
make


echo ""
echo ""
echo "--- cd CDStructure---"

cd ../CDStructure
make clean
make


#echo ""
#echo ""
#echo "--- cd CDThread ---"

cd ../CDThread
make clean
make


