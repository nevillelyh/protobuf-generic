#!/bin/bash

set -euo pipefail

PROTO=2.6.1
OUT="proto2test/src/test/proto-$PROTO"
mkdir -p "$OUT"
./protoc.sh $PROTO --java_out="$OUT" proto2test/src/test/protobuf/*.proto

for PROTO in 3.15.8 3.16.0 3.17.3; do
    OUT="proto2test/src/test/proto-$PROTO"
    mkdir -p "$OUT"
    ./protoc.sh $PROTO --java_out="$OUT" proto2test/src/test/protobuf/*.proto

    OUT="proto3test/src/test/proto-$PROTO"
    mkdir -p "$OUT"
    ./protoc.sh $PROTO --java_out="$OUT" proto3test/src/test/protobuf/*.proto
done
