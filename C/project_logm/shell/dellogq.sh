echo "LOG_QUEUE_DELETER"
echo "START"

ipcrm -M 0x12001

ipcrm -Q 0x10001
ipcrm -Q 0x10002
ipcrm -Q 0x10003
ipcrm -Q 0x10004
ipcrm -Q 0x10005
ipcrm -Q 0x10006
ipcrm -Q 0x10007
ipcrm -Q 0x10008
ipcrm -Q 0x10009
ipcrm -Q 0x1000a
ipcrm -Q 0x1000b
ipcrm -Q 0x1000c
ipcrm -Q 0x1000d
ipcrm -Q 0x1000e
ipcrm -Q 0x1000f
ipcrm -Q 0x10010

ipcrm -Q 0x11001
ipcrm -Q 0x11002
ipcrm -Q 0x11003
ipcrm -Q 0x11004
ipcrm -Q 0x11005
ipcrm -Q 0x11006
ipcrm -Q 0x11007
ipcrm -Q 0x11008
ipcrm -Q 0x11009
ipcrm -Q 0x1100a
ipcrm -Q 0x1100b
ipcrm -Q 0x1100c
ipcrm -Q 0x1100d
ipcrm -Q 0x1100e
ipcrm -Q 0x1100f
ipcrm -Q 0x11010

/bin/rm -f /data/log/logmgr/log/*
/bin/rm -f /data/log/logmgr/data/*
/bin/rm -f /data/log/logmgr/guts/*


echo "END"
