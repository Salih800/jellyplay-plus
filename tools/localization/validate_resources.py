"""Validate a complete resource translation without third-party dependencies.

Run from the repository root: python tools/localization/validate_resources.py tr
Checks the XML structure, resource keys/types, format arguments, plural
quantities, attributes and module/file coverage against the default resources.
"""
from collections import Counter
from pathlib import Path
import argparse
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
FORMAT = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z%]")


def entries(path):
    root = ET.parse(path).getroot()
    if root.tag != "resources":
        raise ValueError(f"Unexpected root in {path}")
    result = {}
    for element in root:
        if element.tag not in ("string", "plurals"):
            continue
        if element.get("translatable") == "false":
            continue
        name = element.get("name")
        if not name or name in result:
            raise ValueError(f"Missing or duplicate name in {path}: {name}")
        result[name] = element
    return result


def text_contract(default, localized, label, errors):
    source = "".join(default.itertext())
    target = "".join(localized.itertext())
    if source.strip() and not target.strip():
        errors.append(f"{label}: empty translation")
    if Counter(FORMAT.findall(source)) != Counter(FORMAT.findall(target)):
        errors.append(f"{label}: format arguments differ")
    for token in ("\\n", "\\t", "\\r"):
        if source.count(token) != target.count(token):
            errors.append(f"{label}: {token} escapes differ")
    if re.search(r"__PH_\d+__|\[J\d{4}\]", target):
        errors.append(f"{label}: draft translation marker remains")


def validate(locale):
    errors = []
    files = strings = plurals = 0
    for top in ("app", "shared"):
        for source_path in sorted((ROOT / top).rglob("values/*.xml")):
            if "build" in source_path.relative_to(ROOT).parts:
                continue
            source = entries(source_path)
            if not source:
                continue
            files += 1
            target_path = source_path.parent.with_name(f"values-{locale}") / source_path.name
            label = str(target_path.relative_to(ROOT))
            if not target_path.exists():
                errors.append(f"{label}: missing file")
                continue
            target = entries(target_path)
            if source.keys() != target.keys():
                errors.append(f"{label}: missing={sorted(source.keys() - target.keys())}, "
                              f"extra={sorted(target.keys() - source.keys())}")
            for name in source.keys() & target.keys():
                a, b = source[name], target[name]
                key = f"{label}:{name}"
                if a.tag != b.tag or a.attrib != b.attrib:
                    errors.append(f"{key}: type or attributes differ")
                    continue
                if a.tag == "string":
                    strings += 1
                    text_contract(a, b, key, errors)
                else:
                    plurals += 1
                    a_items = {e.get("quantity"): e for e in a}
                    b_items = {e.get("quantity"): e for e in b}
                    if a_items.keys() != b_items.keys() or "other" not in b_items:
                        errors.append(f"{key}: plural quantities differ or lack 'other'")
                    for quantity in a_items.keys() & b_items.keys():
                        text_contract(a_items[quantity], b_items[quantity], f"{key}[{quantity}]", errors)
    if errors:
        print("\n".join(errors))
        print(f"FAIL: {len(errors)} resource contract errors")
        return 1
    print(f"PASS: {locale}: {files} files, {strings} strings, {plurals} plural resources; complete coverage")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("locale", nargs="?", default="tr")
    args = parser.parse_args()
    try:
        sys.exit(validate(args.locale))
    except (ET.ParseError, ValueError) as error:
        print(f"FAIL: {error}")
        sys.exit(1)
