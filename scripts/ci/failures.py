#!/usr/bin/env python3
"""Prints failing test names and messages from JUnit XML, for committed CI evidence."""
import glob
import xml.etree.ElementTree as ET

count = 0
for path in glob.glob("**/build/test-results/**/TEST-*.xml", recursive=True):
    try:
        root = ET.parse(path).getroot()
    except Exception:
        continue
    for case in root.iter("testcase"):
        for fail in list(case.iter("failure")) + list(case.iter("error")):
            msg = (fail.get("message") or "").strip().replace("\n", " ")
            print(f"{case.get('classname')}.{case.get('name')}: {msg[:300]}")
            count += 1
# CI prints "no failures" via the fallback only when this list is empty.
if count == 0:
    print("(none)")
