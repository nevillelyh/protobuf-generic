#!/bin/bash

set -euo pipefail

if [ $# -lt 2 ]; then
    echo "Usage: protoc.sh VERSION [ARG]..."
    exit 1
fi

VERSION=$1
shift

OS=$(uname -s)
case "$OS" in
    Linux)
        OS="linux"
        CACHE="$HOME/.cache/protoc.sh"
        ;;
    Darwin)
        OS="osx"
        CACHE="$HOME/Library/Caches/protoc.sh"
        ;;
    *)
        echo "Unknown operating system: $OS"
        exit 1
esac

ARCH=$(uname -m)

if [ "$OS" == "osx" ] && [ "$ARCH" == "arm64" ]; then
    ARCH=x86_64
fi

PREFIX="https://github.com/protocolbuffers/protobuf/releases/download"
ZIP="protoc-$VERSION-$OS-$ARCH.zip"
URL="$PREFIX/v$VERSION/$ZIP"
PROTOC_DIR="$CACHE/$VERSION"

if [ ! -d "$PROTOC_DIR" ]; then
    ARCHIVES="$CACHE/archives"
    mkdir -p "$ARCHIVES"
    curl -sL "$URL" -o "$ARCHIVES/$ZIP"
    unzip "$ARCHIVES/$ZIP" -d "$PROTOC_DIR"
fi

"$PROTOC_DIR/bin/protoc" $*
