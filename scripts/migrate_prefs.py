#!/usr/bin/env python3
"""
Preference Migration Script for Aournal++
Migrates legacy shared preferences XML files to standardized new names via ADB.

Legacy -> New:
- aournal_prefs.xml -> aournalpp_general.xml
- aournal_doc_hub_prefs.xml -> aournalpp_document_hub.xml
- x11_preferences.xml -> aournalpp_x11.xml
- aournal_cloud_backup_prefs.xml -> aournalpp_backup.xml
"""

import subprocess
import sys

PACKAGE = "dev.ilamparithi.aournalpp"
PREF_DIR = f"/data/data/{PACKAGE}/shared_prefs"

MAPPINGS = [
    ("aournal_prefs.xml", "aournalpp_general.xml"),
    ("aournal_doc_hub_prefs.xml", "aournalpp_document_hub.xml"),
    ("x11_preferences.xml", "aournalpp_x11.xml"),
    ("aournal_cloud_backup_prefs.xml", "aournalpp_backup.xml"),
]

def run_adb(cmd: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(["adb"] + cmd, capture_output=True, text=True)

def main():
    print(f"Checking ADB devices for package {PACKAGE}...")
    devices = run_adb(["devices"])
    if devices.returncode != 0 or not devices.stdout.strip():
        print("Error: adb command failed or no devices found.")
        sys.exit(1)

    print("Stopping application...")
    run_adb(["shell", f"am force-stop {PACKAGE}"])

    for old_name, new_name in MAPPINGS:
        old_path = f"{PREF_DIR}/{old_name}"
        new_path = f"{PREF_DIR}/{new_name}"
        check = run_adb(["shell", f"run-as {PACKAGE} test -f {old_path} && echo EXISTS"])
        if "EXISTS" in check.stdout:
            print(f"Migrating {old_name} -> {new_name}...")
            run_adb(["shell", f"run-as {PACKAGE} cp {old_path} {new_path}"])
        else:
            print(f"Skipping {old_name} (not present)")

    print("Migration complete.")

if __name__ == "__main__":
    main()
