#!/bin/bash

# Usage check
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <prefix> <number_of_consumers>"
    exit 1
fi

prefix="$1"
N="$2"

for i in $(seq 1 "$N"); do
    stream_name="p${i}"
    consumer_name="${prefix}_${stream_name}_con"
    config_file="con_${consumer_name}.json"

    cat <<EOF > "$config_file"
{
  "ack_policy": "explicit",
  "ack_wait": 120000000000,
  "deliver_policy": "all",
  "durable_name": "$consumer_name",
  "filter_subject": ">",
  "max_ack_pending": 100000,
  "max_deliver": 3,
  "max_waiting": 512,
  "replay_policy": "instant"
}
EOF

    echo "Prepared config for consumer '$consumer_name' on stream '$stream_name' ($config_file)"

    # Uncomment the line below to actually create the consumer
    nats consumer add "$stream_name" --config "$config_file"

    rm -f "$config_file"
    echo "Removed temporary file: $config_file"
done

echo "Finished creating $N consumers for prefix '$prefix'."
