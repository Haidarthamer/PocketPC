#!/usr/bin/env python3
"""Aggregates JUnit XML into one totals line for the committed CI evidence."""
import glob
import xml.etree.ElementTree as ET

suites = tests = failures = errors = skipped = 0
for path in glob.glob("**/build/test-results/**/TEST-*.xml", recursive=True):
    try:
        root = ET.parse(path).getroot()
    except Exception:
        continue
    tag = root if root.tag == "testsuite" else root.find("testsuite")
    if tag is None:
        continue
    suites += 1
    tests += int(tag.get("tests", 0) or 0)
    failures += int(tag.get("failures", 0) or 0)
    errors += int(tag.get("errors", 0) or 0)
    skipped += int(tag.get("skipped", 0) or 0)

print(f"{suites} test suites, {tests} tests, {failures} failures, {errors} errors, {skipped} skipped")
if failures or errors:
    raise SystemExit(1)
