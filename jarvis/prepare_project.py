import base64,os,pathlib,shutil,xml.etree.ElementTree as ET,re
root=pathlib.Path(__file__).parent
out=pathlib.Path('generated');shutil.copytree(root/'template',out)
for name in ['settings.gradle.kts','build.gradle.kts','gradle.properties']:shutil.copy(root/name,out/name)
html=base64.b64decode(os.environ['PROJECT_HTML'],validate=True)
assert len(html)<=45000,'HTML too large'
pkg=os.environ['PROJECT_PACKAGE'];assert re.fullmatch(r'[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*){2,}',pkg),'Invalid package'
(out/'app/src/main/assets').mkdir(exist_ok=True)
(out/'app/src/main/assets/index.html').write_bytes(html)
p=out/'app/build.gradle.kts';p.write_text(p.read_text().replace('applicationId="com.mazhar.generated"','applicationId="'+pkg+'"'))
p=out/'app/src/main/AndroidManifest.xml';ET.register_namespace('android','http://schemas.android.com/apk/res/android');tree=ET.parse(p);tree.getroot().find('application').set('{http://schemas.android.com/apk/res/android}label',os.environ['PROJECT_NAME'][:60]);tree.write(p,encoding='utf-8',xml_declaration=True)
