#!/usr/bin/env python3
"""Bounded ZIP extraction and deterministic project-root detection."""
import os, stat, sys, zipfile
from pathlib import Path

def extract(archive, destination):
    root = Path(destination).resolve(); root.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(archive) as z:
        entries = z.infolist()
        if len(entries) > 20000: raise ValueError("ZIP contains too many entries (maximum 20000)")
        total = 0
        for e in entries:
            if "\\" in e.filename: raise ValueError("Unsupported ZIP path")
            target = (root / e.filename).resolve()
            if not target.is_relative_to(root): raise ValueError("Unsafe ZIP path")
            if stat.S_ISLNK(e.external_attr >> 16): raise ValueError("ZIP symlinks are not supported")
            total += e.file_size
            if total > 1024 * 1024 * 1024: raise ValueError("Expanded project exceeds 1 GB")
            if e.is_dir(): target.mkdir(parents=True, exist_ok=True); continue
            target.parent.mkdir(parents=True, exist_ok=True)
            with z.open(e) as source, target.open("wb") as out:
                written = 0
                while chunk := source.read(65536):
                    written += len(chunk)
                    if written > e.file_size: raise ValueError("ZIP size mismatch")
                    out.write(chunk)
    return root

def kind(root):
    if (root / "pubspec.yaml").is_file() and (root / "lib/main.dart").is_file(): return "FLUTTER"
    if (root / "settings.gradle").is_file() or (root / "settings.gradle.kts").is_file(): return "ANDROID"
    if (root / "project.godot").is_file(): return "GODOT"
    if (root / "package.json").is_file(): return "NODE_WEB"
    if (root / "index.html").is_file(): return "WEB"

def detect(root):
    for _ in range(8):
        k = kind(root)
        if k: return root, k
        children = [p for p in root.iterdir() if p.name not in {"__MACOSX", ".DS_Store", ".git"}]
        dirs = [p for p in children if p.is_dir()]
        # Permit a README alongside one enclosing project directory.
        files = [p for p in children if p.is_file() and not p.name.lower().startswith(("readme", "license"))]
        if len(dirs) != 1 or files: break
        root = dirs[0]
    raise ValueError("No unambiguous supported project root: include index.html, package.json, Android settings.gradle, or Flutter pubspec.yaml + lib/main.dart")

if __name__ == "__main__":
    try:
        root, project_type = detect(extract(sys.argv[1], sys.argv[2]))
        with open(os.environ["GITHUB_ENV"], "a") as f:
            f.write(f"PROJECT_ROOT={root}\nPROJECT_TYPE={project_type}\n")
        print(f"Detected {project_type}")
    except Exception as e:
        sys.exit(str(e))
