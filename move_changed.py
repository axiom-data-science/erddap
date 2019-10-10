#! /usr/bin/env python35
import os
import shutil
import subprocess

# export ERDDAP_DEV_ROOT=/data/erddap/code/erddap
# export ERDDAP_UPSTREAM_ROOT=/data/erddap/code/erddap-bob


dev = os.environ['ERDDAP_DEV_ROOT']
upstream = os.environ['ERDDAP_UPSTREAM_ROOT']

previous_version = '8a1e8e7'
new_version = 'HEAD'

files = subprocess.run(['git', 'diff', '--name-status', previous_version, new_version], stdout=subprocess.PIPE, cwd=upstream)
for code, contents in [ x.split('\t', maxsplit=1) for x in files.stdout.decode('utf-8').split('\n') if x ]:

    if code == 'D':
        print('{} was deleted, please remove manually'.format(contents))
    elif code.startswith('R'):
        print('{} was renamed, please rename manually.'.format(contents))
    elif code == 'C':
        print('{} was copied onto another, please perform manually'.format(contents))
    elif code in ['A', 'M']:
        filename = os.path.basename(contents)
        new_path = None
        if contents.startswith('download/'):
            new_path = contents.replace('download/', os.path.join(dev, 'src/main/webapp/download/'), 1)
        elif contents.startswith('WEB-INF/classes/'):
            new_path = contents.replace('WEB-INF/classes/', os.path.join(dev, 'src/main/java/'), 1)
        elif contents.startswith('WEB-INF/images/'):
            new_path = contents.replace('WEB-INF/images/', os.path.join(dev, 'src/main/webapp/WEB-INF/images/'), 1)
        elif contents.startswith('images/'):
            new_path = contents.replace('images/', os.path.join(dev, 'src/main/webapp/images/'), 1)
        elif contents.startswith('WEB-INF/'):
            new_path = contents.replace('WEB-INF/', os.path.join(dev, 'src/main/webapp/WEB-INF/'), 1)

        if new_path is not None:
            try:
                os.makedirs(os.path.dirname(new_path))
            except OSError:
                pass  # exists
            old_path = os.path.join(upstream, contents)
            #print('{} -> {}'.format(contents, new_path))
            shutil.copy2(old_path, new_path)

    else:
        print('Can not handle the {} action, please perform manually: {}'.format(code, contents))
