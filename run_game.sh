#!/bin/sh

set -e

javac MyBot.java
./halite --replay-directory replays/ -vvv --width 32 --height 32 "java MyBot" "java MyBot"

rm -rf *.class
rm -rf hlt/*.class

