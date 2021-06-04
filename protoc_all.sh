#!/bin/bash

set -euo pipefail

for PROTO in 3.15.8 3.16.0 3.17.2; do
    OUT="proto2test/src/test/proto-$PROTO"
    mkdir -p "$OUT"
    ./protoc.sh $PROTO --java_out="$OUT" proto2test/src/test/protobuf/*.proto

    OUT="proto3test/src/test/proto-$PROTO"
    mkdir -p "$OUT"
    ./protoc.sh $PROTO --java_out="$OUT" proto3test/src/test/protobuf/*.proto
done
