#!/bin/bash

# Usage: ./create_stream.sh STREAM_NAME SUBJECT

set -e

if [ $# -ne 2 ]; then
  echo "Usage: $0 STREAM_NAME SUBJECT"
  exit 1
fi

STREAM_NAME="$1"
STREAM_SUBJECT="$2"
TMP_JSON="stream_config_${STREAM_NAME}.json"

cat > "$TMP_JSON" <<EOF
{
  "config": {
    "name": "${STREAM_NAME}",
    "subjects": [
      "${STREAM_SUBJECT}"
    ],
    "retention": "limits",
    "max_consumers": -1,
    "max_msgs_per_subject": -1,
    "max_msgs": 5000000,
    "max_bytes": -1,
    "max_age": 1200000000000,
    "max_msg_size": -1,
    "storage": "file",
    "discard": "old",
    "num_replicas": 3,
    "duplicate_window": 120000000000,
    "sealed": false,
    "deny_delete": false,
    "deny_purge": false,
    "allow_rollup_hdrs": true,
    "allow_direct": true,
    "mirror_direct": false,
    "consumer_limits": {}
  }
}
EOF

echo "Creating stream '${STREAM_NAME}' with subject '${STREAM_SUBJECT}'..."

nats stream add --config "$TMP_JSON"

# Optionally, clean up the json file
rm "$TMP_JSON"
