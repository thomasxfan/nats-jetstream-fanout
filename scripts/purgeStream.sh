#!/bin/bash

# Usage: ./purgeStream.sh <NUMBER_OF_STREAMS>
# Example: ./purgeStream.sh 4

set -e

if [ $# -ne 1 ]; then
  echo "Usage: $0 <NUMBER_OF_STREAMS>"
  exit 1
fi

NUMBER_OF_STREAMS="$1"

if ! [[ "$NUMBER_OF_STREAMS" =~ ^[0-9]+$ ]] || [ "$NUMBER_OF_STREAMS" -le 0 ]; then
  echo "NUMBER_OF_STREAMS must be a positive integer."
  exit 1
fi

for ((i=0; i<NUMBER_OF_STREAMS; i++)); do
  STREAM_NAME="p$((i+1))"
  echo "purge stream $STREAM_NAME "
  nats s purge $STREAM_NAME --force
done

echo "All $NUMBER_OF_STREAMS streams purged."
