#!/bin/bash

# Usage: ./delete_many_streams.sh <NUMBER_OF_STREAMS>
# Example: ./delete_many_streams.sh 100
# This will delete streams p1 to p100

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

for ((i=1; i<=NUMBER_OF_STREAMS; i++)); do
  STREAM_NAME="p$i"
  echo "Deleting stream $STREAM_NAME..."
  nats s delete "$STREAM_NAME" --force
done

echo "All $NUMBER_OF_STREAMS streams deleted."
