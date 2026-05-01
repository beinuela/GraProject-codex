from __future__ import annotations

import argparse
import pathlib
import re
import shutil
import sys
import zipfile
from datetime import datetime


UPDATE_FIELDS_PATTERN = re.compile(r"<w:updateFields\b[^>]*/>")
SUSPICIOUS_FIELD_KEYWORDS = (
    "INCLUDETEXT",
    "INCLUDEPICTURE",
    " DDEAUTO",
    " LINK ",
)


def find_field_codes(docx_path: pathlib.Path) -> list[str]:
    fields: list[str] = []
    with zipfile.ZipFile(docx_path) as zf:
        for xml_name in [name for name in zf.namelist() if name.startswith("word/") and name.endswith(".xml")]:
            xml = zf.read(xml_name).decode("utf-8", "ignore")
            for match in re.findall(r"<w:instrText[^>]*>(.*?)</w:instrText>", xml):
                normalized = re.sub(r"\s+", " ", match).strip()
                if normalized:
                    fields.append(normalized)
    return fields


def create_backup(docx_path: pathlib.Path) -> pathlib.Path:
    stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
    backup_path = docx_path.with_name(f"{docx_path.name}.bak.{stamp}-unlink-fields")
    shutil.copy2(docx_path, backup_path)
    return backup_path


def strip_update_fields(docx_path: pathlib.Path) -> bool:
    updated = False
    with zipfile.ZipFile(docx_path, "r") as src:
        entries = [(item, src.read(item.filename)) for item in src.infolist()]

    tmp_path = docx_path.with_suffix(docx_path.suffix + ".tmp")
    with zipfile.ZipFile(tmp_path, "w", compression=zipfile.ZIP_DEFLATED) as dst:
        for item, data in entries:
            if item.filename == "word/settings.xml":
                text = data.decode("utf-8", "ignore")
                new_text, count = UPDATE_FIELDS_PATTERN.subn("", text)
                if count:
                    updated = True
                    data = new_text.encode("utf-8")
            dst.writestr(item, data)

    tmp_path.replace(docx_path)
    return updated


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Remove the Word 'update fields on open' flag from a DOCX so Word stops prompting about fields."
    )
    parser.add_argument("docx_path", type=pathlib.Path)
    args = parser.parse_args()

    docx_path = args.docx_path.resolve()
    if not docx_path.exists():
        print(f"ERROR: file not found: {docx_path}", file=sys.stderr)
        return 1

    fields = find_field_codes(docx_path)
    suspicious = [field for field in fields if any(keyword in field for keyword in SUSPICIOUS_FIELD_KEYWORDS)]
    if suspicious:
        print("WARNING: suspicious field codes detected:")
        for field in suspicious:
            print(f"  {field}")
    else:
        print("No external-file field codes detected; only internal Word fields remain.")

    backup_path = create_backup(docx_path)
    updated = strip_update_fields(docx_path)
    print(f"Backup created: {backup_path}")
    print(f"Updated settings.xml: {updated}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
