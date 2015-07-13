#!/bin/bash

echo "Setting up server"

ffserver -f /etc/ffserver.conf &
xterm -hold -e "ffmpeg -f v4l2 -s 640x480 -r 10 -i /dev/video1 http://localhost:8080/feed1.ffm"