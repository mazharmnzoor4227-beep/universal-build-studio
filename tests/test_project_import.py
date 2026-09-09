import importlib.util, tempfile, unittest, zipfile
from pathlib import Path
spec = importlib.util.spec_from_file_location("prepare", Path(__file__).parents[1]/"scripts/prepare-project.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
class ImportTests(unittest.TestCase):
    def test_nested_web(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d); z=p/"input.zip"
            with zipfile.ZipFile(z,"w") as a: a.writestr("project/index.html","hello")
            root, kind=m.detect(m.extract(z,p/"out")); self.assertEqual(kind,"WEB"); self.assertEqual(root.name,"project")
    def test_traversal(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d);z=p/"input.zip"
            with zipfile.ZipFile(z,"w") as a:a.writestr("../escape","no")
            with self.assertRaises(ValueError):m.extract(z,p/"out")
    def test_flutter_before_android(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d);(p/"lib").mkdir();(p/"lib/main.dart").touch();(p/"pubspec.yaml").touch();(p/"settings.gradle").touch()
            self.assertEqual(m.detect(p)[1],"FLUTTER")
    def test_ambiguous(self):
        with tempfile.TemporaryDirectory() as d:
            p=Path(d);(p/"a").mkdir();(p/"b").mkdir()
            with self.assertRaises(ValueError):m.detect(p)
if __name__ == "__main__":unittest.main()
