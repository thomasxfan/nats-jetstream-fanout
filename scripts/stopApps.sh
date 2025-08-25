#!/bin/bash

# Stop script for the 'consume' Java process and remove logs

PIDS=$(ps aux | grep '[j]ava.*consume' | awk '{print $2}')

if [[ -z "$PIDS" ]]; then
    echo "No running 'consume' process found."
else
    echo "Found consume process(es) with PID(s): $PIDS"
    for PID in $PIDS; do
        echo "Stopping process $PID..."
        kill "$PID"
    done
    echo "All consume process(es) stopped."
fi

# Remove all log files in the logs directory
if [[ -d logs ]]; then
    echo "Removing all log files in the logs directory..."
    rm -f logs/consume*.log
    echo "All logs removed from logs/."
else
    echo "No logs directory to remove."
fi

echo "Done."
