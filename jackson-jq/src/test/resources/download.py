# coding: utf-8

import json
import yaml
from urllib import request as urllib2

def load_jq_tests_from_url(url):
    result = []
    lines = urllib2.urlopen(url).read().decode('utf-8').split('\n')
    print(lines)
    ite = iter(lines)
    while True:
        try:
            line = next(ite)
        except StopIteration: break
        if not line or line.startswith('#'):
            continue

        # if line.strip() == "%%FAIL":
        if line.strip().startswith("%%FAIL"):
            while line:
                try:
                    line = next(ite)
                except StopIteration: break
            continue

        _q = line

        line = next(ite)
        print(line)
        _in = json.loads(line.replace('-Infinity', 'null') \
                .replace('Infinity', 'null') \
                .replace('-NaN', 'null') \
                .replace('NaN', 'null') \
                .replace(u'\ufeff', u' '))

        _out = []
        while True:
            try:
                line = next(ite).replace('\ufeff', ' ')
            except StopIteration: break
            if not line or line.startswith('#'):
                break
            _out.append(json.loads(line))
        result.append({'q': _q, 'in': _in, 'out': _out})
    return result

def load_jq_manual():
    with urllib2.urlopen('https://raw.githubusercontent.com/stedolan/jq/master/docs/content/3.manual/manual.yml') as f:
        manual = yaml.load(f)

    result = []
    for section in manual['sections']:
        for entry in section.get('entries', []):
            for example in entry.get('examples', []):
                print('Example:', example)

                out = []
                for obj in example['output']:
                    if not isinstance(obj, str):
                        out.append(obj)
                    else:
                        out.append(json.loads(obj))

                if not isinstance(example['input'], str):
                    in_ = example['input']
                else:
                    in_ = json.loads(example['input'])

                r = {
                    'q': example['program'],
                    'in': in_,
                    'out': out
                }
                result.append(r)
                print(r)
    return result

if __name__ == '__main__':
    with open('exclude.lst') as f:
        excludes = set(line.strip() for line in f if line and not line.startswith('#'))

    def emit_tests(cases, fname_ok, fname_ng):
        with open(fname_ok, 'w') as f:
            json.dump(list(filter(lambda x: x['q'] not in excludes, cases)), f, indent=4, sort_keys=True)
        with open(fname_ng, 'w') as f:
            json.dump(list(filter(lambda x: x['q'] in excludes, cases)), f, indent=4, sort_keys=True)

    emit_tests(load_jq_tests_from_url('https://raw.githubusercontent.com/stedolan/jq/master/tests/jq.test'), 'jq-test-all-ok.json', 'jq-test-all-ng.json')
    emit_tests(load_jq_tests_from_url('https://raw.githubusercontent.com/stedolan/jq/master/tests/onig.test'), 'jq-test-onig-ok.json', 'jq-test-onig-ng.json')
    emit_tests(load_jq_manual(), 'jq-test-manual-ok.json', 'jq-test-manual-ng.json')

