#!/bin/bash

set -euo pipefail

if [ $# -lt 1 ]; then
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

PROTOC_DIR="$CACHE/$VERSION"
ARCHIVES="$CACHE/archives"
PREFIX="https://github.com/protocolbuffers/protobuf/releases/download"

echo $VERSION
if [[ "$VERSION" = 2\.* ]]; then
    BZ2="protobuf-$VERSION.tar.bz2"
    URL="$PREFIX/v$VERSION/$BZ2"
    if [ ! -d "$PROTOC_DIR" ]; then
        mkdir -p "$PROTOC_DIR"
        echo $URL
        curl -sL "$URL" | tar -xzf -C $PROTOC_DIR -
    fi
else
    ZIP="protoc-$VERSION-$OS-$ARCH.zip"
    URL="$PREFIX/v$VERSION/$ZIP"
    if [ ! -d "$PROTOC_DIR" ]; then
        mkdir -p "$ARCHIVES"
        curl -sL "$URL" -o "$ARCHIVES/$ZIP"
        unzip "$ARCHIVES/$ZIP" -d "$PROTOC_DIR"
    fi
fi

exit
"$PROTOC_DIR/bin/protoc" $*
