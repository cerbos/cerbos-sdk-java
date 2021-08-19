#!/usr/bin/env bash
#
# Copyright 2021 Zenauth Ltd.

set -euo pipefail

CERBOS_MODULE=${CERBOS_MODULE:-"buf.build/cerbos/cerbos-api"}
TMP_PROTO_DIR="$(mktemp -d -t cerbos-XXXXX)"

trap 'rm -rf "$TMP_PROTO_DIR"' EXIT

buf mod update
buf export "$CERBOS_MODULE" -o "$TMP_PROTO_DIR"

rm -rf src/main/proto
mv "$TMP_PROTO_DIR" src/main/proto

