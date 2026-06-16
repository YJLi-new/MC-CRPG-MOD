#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
python3 -m py_compile \
  scripts/gui_e2e_run.py \
  scripts/install_gui_automation_deps.py \
  scripts/check_pcl_runtime_loaded.py \
  scripts/gui_retest_issue_audit.py \
  tools/gui_automation/python/*.py
npm --prefix tools/gui_automation/node run self-test
scripts/gui_e2e_run.py --scenario dry_run >/tmp/ebb_gui_automation_dry_run.json
scripts/gui_e2e_run.py --scenario llm_chat --allow-stale-runtime >/tmp/ebb_gui_automation_llm_chat.json
scripts/gui_e2e_run.py --scenario runtime_check >/tmp/ebb_gui_automation_runtime_check.txt || true
printf 'GUI automation smoke passed. Dry-run report: /tmp/ebb_gui_automation_dry_run.json\n'
