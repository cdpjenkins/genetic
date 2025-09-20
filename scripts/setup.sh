#!/bin/sh

apt update
apt install openjdk-21-jdk

cat >.screenrc <<END
escape ^Tt
END
