#!/usr/bin/env bash
# Generates a steady mix of normal, slow, and failing requests against the
# java-monitor-demo app, so there's always traffic to look at in Kibana.
#
# Usage: ./generate-traffic.sh [host:port] [requests-per-cycle-of-each-kind]
#
# Defaults to localhost:8080 and 1 request of each kind every ~2 seconds.
# Stop with Ctrl+C.

set -u

HOST="${1:-localhost:8080}"
ITEMS=(mug t-shirt sticker-pack)

echo "Generating traffic against http://${HOST} (Ctrl+C to stop)"

while true; do
  item="${ITEMS[$RANDOM % ${#ITEMS[@]}]}"

  # Mostly normal requests, with slow and failing requests mixed in so
  # traces/logs/metrics show a realistic distribution rather than 100%
  # errors or 100% latency.
  roll=$((RANDOM % 10))

  if [ "$roll" -lt 6 ]; then
    curl -s -o /dev/null -w "greeting [%{http_code}] item=${item}\n" "http://${HOST}/greeting?item=${item}"
  elif [ "$roll" -lt 8 ]; then
    curl -s -o /dev/null -w "slow     [%{http_code}] item=${item}\n" "http://${HOST}/slow?item=${item}"
  else
    curl -s -o /dev/null -w "error    [%{http_code}] item=${item}\n" "http://${HOST}/error?item=${item}"
  fi

  sleep 2
done
