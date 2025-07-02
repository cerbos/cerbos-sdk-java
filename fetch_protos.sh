#!/usr/bin/env bash
#
# Copyright 2021-2025 Zenauth Ltd.
# SPDX-License-Identifier: Apache-2.0
#

set -euo pipefail

CERBOS_MODULE=${CERBOS_MODULE:-"buf.build/cerbos/cerbos-api"}
CLOUD_API_MODULE=${CLOUD_API_MODULE:-"buf.build/cerbos/cloud-api"}
TMP_PROTO_DIR="$(mktemp -d -t cerbos-XXXXX)"

trap 'rm -rf "$TMP_PROTO_DIR"' EXIT

buf export "$CLOUD_API_MODULE" --output="$TMP_PROTO_DIR"
buf export "$CERBOS_MODULE" --output="$TMP_PROTO_DIR"
buf export "buf.build/bufbuild/protovalidate" --output="$TMP_PROTO_DIR"

rm -rf src/main/proto
mv "$TMP_PROTO_DIR" src/main/proto
