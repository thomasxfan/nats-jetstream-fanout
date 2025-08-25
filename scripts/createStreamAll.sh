#!/bin/bash

# Usage: ./createStreamAll.sh <NUMBER_OF_STREAMS>
# Example: ./create_many_streams.sh 4
# Will create streams: p1, p2, p3, p4 with subjects p.user.messages.0 ... p.user.messages.3

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
  SUBJECT="p.user.messages.$i"
  echo "Creating stream $STREAM_NAME with subject $SUBJECT..."
  ./create_stream.sh "$STREAM_NAME" "$SUBJECT"
done

echo "All $NUMBER_OF_STREAMS streams created."
