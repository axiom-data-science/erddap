#! /usr/bin/env python35
import os
import shutil
import subprocess

# export ERDDAP_DEV_ROOT=/data/erddap/code/erddap
# export ERDDAP_UPSTREAM_ROOT=/data/erddap/code/erddap-bob


dev = os.environ['ERDDAP_DEV_ROOT']
upstream = os.environ['ERDDAP_UPSTREAM_ROOT']

files = subprocess.run(['git', 'diff', '--name-status', 'HEAD~2', 'HEAD'], stdout=subprocess.PIPE, cwd=upstream)
for code, contents in [ x.split('\t', maxsplit=2) for x in files.stdout.decode('utf-8').split('\n') if x ]:

    if code == 'D':
        print('{} was deleted, please remove manually'.format(contents))
    elif code == 'R':
        print('{} was renamed, please rename manually.'.format(contents))
    elif code == 'C':
        print('{} was copied onto another, please perform manually'.format(contents))
    elif code in ['A', 'M']:
        filename = os.path.basename(contents)
        new_path = None
        if contents.startswith('download/'):
            continue
        elif contents.startswith('WEB-INF/classes/'):
            new_path = contents.replace('WEB-INF/classes/', os.path.join(dev, 'src/main/java/'))
        elif contents.startswith('WEB-INF/images/'):
            new_path = contents.replace('WEB-INF/images/', os.path.join(dev, 'src/main/webapp/images/'))
        elif contents.startswith('WEB-INF/'):
            new_path = contents.replace('WEB-INF/', os.path.join(dev, 'src/main/webapp/WEB-INF/'))

        if new_path is not None:
            try:
                os.makedirs(os.path.dirname(new_path))
            except OSError:
                pass  # exists
            old_path = os.path.join(upstream, contents)
            print('{} -> {}'.format(contents, new_path))
            shutil.copy2(old_path, new_path)

    else:
        print('Can not handle the {} action, please perform manually: {}'.format(code, contents))
