#!/bin/bash
mvn install
status=$?
if [ $status -eq 0 ];then 
	echo "################ Maven Install successful #################"
	java -jar target/video-quickstart-1.0-SNAPSHOT.jar
else
	echo "********** Maven Install failed ***********"
fi
