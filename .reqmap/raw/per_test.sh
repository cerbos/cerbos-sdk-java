#!/usr/bin/env bash
# Per-test-method JaCoCo slices, each tagged with its tests.json id, merged into .reqmap/coverage.json.
# CerbosClientTests is abstract, so its methods run through CerbosBlockingClientTest.
# Cerbos Hub (dev.cerbos.sdk.hub) is out of scope: its tests are not run and its files are dropped.
set -euo pipefail
S="$HOME/.claude/skills/test-coverage-map/scripts"
OUT=.reqmap/raw/slices
rm -rf "$OUT" && mkdir -p "$OUT"
jq -r '.tests[].id' .reqmap/tests.json | while read -r id; do
  file=${id%%::*}
  rest=${id#*::}
  method=${rest##*::}
  cls=${rest%%::*}
  pkg=$(dirname "${file#src/test/java/}" | tr / .)
  case "$cls" in
    CerbosClientTests) run="$pkg.CerbosBlockingClientTest" ;;
    *) run="$pkg.$cls" ;;
  esac
  inner=${rest#"$cls"}; inner=${inner%"::$method"}; inner=${inner#::}
  [ -n "$inner" ] && run="$run\$$inner"
  slug=$(echo "$id" | tr '/.:$' '____')
  rm -f build/jacoco/test.exec
  if ./gradlew test --rerun --tests "$run.$method" jacocoTestReport -q --console=plain >"$OUT/$slug.log" 2>&1; then
    echo "ok    $id"
  else
    # The test failed in isolation (some admin tests depend on method order), but the code
    # it ran is still in test.exec, so build the report from that and keep the slice.
    echo "FAIL  $id (see $OUT/$slug.log); report built from test.exec"
    ./gradlew jacocoTestReport -x test -q --console=plain >>"$OUT/$slug.log" 2>&1
  fi
  python3 "$S/normalise_coverage.py" build/reports/jacoco/test/jacocoTestReport.xml \
    --root . --context "$id" -o "$OUT/$slug.json"
done
python3 "$S/merge_coverage.py" "$OUT/*.json" --root . -o "$OUT/merged.all"
jq '.files |= with_entries(select(.key | startswith("src/main/java/dev/cerbos/sdk/hub/") | not))' \
  "$OUT/merged.all" > "$OUT/merged.nohub"
python3 "$S/merge_coverage.py" "$OUT/merged.nohub" --root . -o .reqmap/coverage.json
python3 "$S/cov_query.py" summary --limit 20
