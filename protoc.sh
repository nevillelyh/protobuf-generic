#!/bin/bash

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: protoc.sh version [arg]..."
    exit 1
fi

version=$1
re='([1-9][0-9]*)\.([1-9][0-9]*)\.(.*)'
[[ "$version" =~ $re ]]
major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"
shift

os=$(uname -s)
case "$os" in
    Linux)
        os="linux"
        cache="$HOME/.cache/protoc.sh"
        ;;
    Darwin)
        os="osx"
        cache="$HOME/Library/Caches/protoc.sh"
        ;;
    *)
        echo "Unknown operating system: $os"
        exit 1
esac

arch=$(uname -m)

# Apple silicon support in 3.20+
if [[ "$os" == "osx" ]] && [[ "$arch" == "arm64" ]]; then
    if [[ "$minor" -lt 20 ]]; then
        arch=x86_64
    else
        arch=aarch_64
    fi
fi

protoc_dir="$cache/$version"
archives="$cache/archives"
prefix="https://github.com/protocolbuffers/protobuf/releases/download"

if [[ "$major" = "2" ]]; then
    if [[ ! -d "$protoc_dir/protobuf-$version" ]]; then
        mkdir -p "$protoc_dir"
        BZ2="protobuf-$version.tar.bz2"
        URL="$prefix/v$version/$BZ2"
        curl -fssL "$URL" | tar -xjf - -C "$protoc_dir"
        CWD=$(pwd)
        cd "$protoc_dir/protobuf-$version"
        ./configure
        make
        cd "$CWD"
    fi
    SRC_DIR="$protoc_dir/protobuf-$version/src"
    protoc="$SRC_DIR/protoc"
    "$protoc" -I"$SRC_DIR" "$@"
else
    if [[ ! -d "$protoc_dir" ]]; then
        mkdir -p "$archives"
        # Decoupled major versions from languages in 3.21+
        if [[ "$major" -eq 3 ]] && [[ "$minor" -ge 21 ]]; then
            version="$minor.$patch"
        elif [[ "$major" -gt 3 ]]; then
            version="$minor.$patch"
        fi
        ZIP="protoc-$version-$os-$arch.zip"
        URL="$prefix/v$version/$ZIP"
        curl -fssL "$URL" -o "$archives/$ZIP"
        unzip "$archives/$ZIP" -d "$protoc_dir"
    fi
    protoc="$protoc_dir/bin/protoc"
    "$protoc" "$@"
fi
