#!/bin/sh

# CronniX
# cd /Users/Mark/src/kendo-emailer/ && ./kendo-emailer.sh

# To get gradle in our PATH
source ~/.bash_profile

echo "Mode = " ${1}

/usr/local/bin/gradle -q run -Dexec.args="${1}"
