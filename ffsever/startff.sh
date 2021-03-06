#!/bin/bash

if [ ! -f "/etc/ffserver.conf" ];
then {
echo "Inserting necessary files"
sudo cp ffserver.conf /etc/
}
fi

echo "Setting up server"

ffserver -f /etc/ffserver.conf &
xterm -hold -e "ffmpeg -f v4l2 -s 640x480 -r 10 -i /dev/video1 http://localhost:8080/feed1.ffm"
