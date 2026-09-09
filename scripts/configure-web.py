#!/usr/bin/env python3
import json, sys, re
from pathlib import Path
import xml.etree.ElementTree as ET
root, metadata = Path(sys.argv[1]), Path(sys.argv[2])
data = json.loads(metadata.read_text()) if metadata.is_file() else {}
options = data.get("options", {})
ns = "{http://schemas.android.com/apk/res/android}"
ET.register_namespace("android", ns[1:-1])
p = root / "app/src/main/AndroidManifest.xml"
tree = ET.parse(p); manifest = tree.getroot()
keep = {"INTERNET"}
if options.get("camera"): keep.add("CAMERA")
if options.get("microphone"): keep.update(["RECORD_AUDIO", "MODIFY_AUDIO_SETTINGS"])
if options.get("library"): keep.update(["READ_MEDIA_AUDIO", "READ_MEDIA_IMAGES", "READ_MEDIA_VIDEO", "READ_EXTERNAL_STORAGE"])
if options.get("media"): keep.update(["FOREGROUND_SERVICE", "FOREGROUND_SERVICE_MEDIA_PLAYBACK", "WAKE_LOCK", "POST_NOTIFICATIONS"])
for e in list(manifest.findall("uses-permission")):
    if e.get(ns + "name", "").split(".")[-1] not in keep: manifest.remove(e)
app = manifest.find("application"); app.set(ns + "usesCleartextTraffic", "false")
app.set(ns + "allowBackup", "false")
activity = app.find("activity")
activity.set(ns + "screenOrientation", "landscape" if options.get("landscape") else "unspecified")
if not options.get("media"):
    for e in list(app.findall("service")): app.remove(e)
tree.write(p, encoding="utf-8", xml_declaration=True)
assets = root / "app/src/main/assets"
(assets / "builder-options.json").write_text(json.dumps(options))
