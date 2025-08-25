#!/bin/bash

# Ensure logs directory exists
mkdir -p logs

# Log file path
LOGFILE="logs/consume2.log"

# Command to run
CMD="java -cp \".:../java/libs/*\" consume 300000 600000 ws2 100"

echo "Running: $CMD"
echo "Logging all output to $LOGFILE"

# Run the command with all output to the log file
eval $CMD > "$LOGFILE" 2>&1 &
