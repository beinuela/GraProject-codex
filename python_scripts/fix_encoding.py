import os
import glob

def fix_encoding(filepath):
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            f.read()
    except UnicodeDecodeError:
        try:
            with open(filepath, 'r', encoding='gbk') as f:
                content = f.read()
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"Fixed encoding: {filepath}")
        except Exception as e:
            print(f"Failed to fix {filepath}: {e}")

vue_files = []
for root, dirs, files in os.walk(r'd:\idea+\project\GraProject-codex\frontend\src'):
    for file in files:
        if file.endswith('.vue') or file.endswith('.js'):
            vue_files.append(os.path.join(root, file))

for f in vue_files:
    fix_encoding(f)
