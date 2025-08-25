#!/bin/bash

# Ensure logs directory exists
mkdir -p logs

# Log file path
LOGFILE="logs/consume3.log"

# Command to run
CMD="java -cp \".:../java/libs/*\" consume 600000 1000000 ws3 100"

echo "Running: $CMD"
echo "Logging all output to $LOGFILE"

# Run the command with all output to the log file
eval $CMD > "$LOGFILE" 2>&1 &
