# coding: utf-8

import json
import yaml
import yaml.nodes
from urllib import request as urllib2
from collections import OrderedDict

class TestCase(object):

    def __init__(self, q, _in, out, v=None, failing=None):
        self.q = q
        self._in = _in
        self.out = out
        self.v = v
        self.failing = failing

    def __yaml__(self, dumper):
        def represent_json(value):
            if value is None:
                return dumper.represent_none(value)
            if isinstance(value, list):
                node = yaml.nodes.SequenceNode('tag:yaml.org,2002:seq', [], flow_style=True)
                for element in value:
                    node.value.append(represent_json(element))
                return node
            if isinstance(value, dict):
                node = yaml.nodes.MappingNode('tag:yaml.org,2002:map', [], flow_style=True)
                for k, v in value.items():
                    node.value.append((represent_json(k), represent_json(v)))
                return node
            if isinstance(value, str):
                return yaml.nodes.ScalarNode('tag:yaml.org,2002:str', value, style='"')
            if isinstance(value, (int, float, bool)):
                node = dumper.represent_data(value)
                node.style = None
                return node
            raise ValueError('unsupported type: ' + type(value))

        root_node = yaml.nodes.MappingNode('tag:yaml.org,2002:map', [], flow_style=False)

        q_node = dumper.represent_data(self.q)
        q_node.style = '\''
        root_node.value.append((dumper.represent_str('q'), q_node))

        root_node.value.append((dumper.represent_str('in'), represent_json(self._in)))

        out_node = yaml.nodes.SequenceNode('tag:yaml.org,2002:seq', [], flow_style=False)
        for o in self.out:
            out_node.value.append(represent_json(o))
        root_node.value.append((dumper.represent_str('out'), out_node))

        if self.v is not None:
            root_node.value.append((dumper.represent_str('v'), dumper.represent_data(self.v)))

        if self.failing is not None:
            root_node.value.append((dumper.represent_str('failing'), dumper.represent_data(self.failing)))

        return root_node

yaml.add_representer(TestCase, lambda dumper, data: data.__yaml__(dumper))

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
            if line.startswith('# Runtime error: '):
                line = '"__ERROR__"'
                _q = 'try (' + _q + ') catch ("__ERROR__")'
            if not line or line.startswith('#'):
                break
            _out.append(json.loads(line))
        result.append(TestCase(_q, _in, _out))
    return result

def load_jq_manual(url):
    with urllib2.urlopen(url) as f:
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

                result.append(TestCase(example['program'], in_, out))
    return result

if __name__ == '__main__':
    with open('exclude.lst') as f:
        excludes = set(line.strip() for line in f if line and not line.startswith('#'))

    def emit_tests(v, cases, fname):
        for case in cases:
            if case.q in excludes:
                case.failing = True
            case.v = v
        with open(fname, 'w') as f:
            f.write('# This file is generated from stedolan/jq files. Different license terms apply:')
            f.write('''
#
# jq is copyright (C) 2012 Stephen Dolan
#
# Permission is hereby granted, free of charge, to any person obtaining
# a copy of this software and associated documentation files (the
# "Software"), to deal in the Software without restriction, including
# without limitation the rights to use, copy, modify, merge, publish,
# distribute, sublicense, and/or sell copies of the Software, and to
# permit persons to whom the Software is furnished to do so, subject to
# the following conditions:
#
# The above copyright notice and this permission notice shall be
# included in all copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
# EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
# NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
# LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
# OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
# WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
#
# jq's documentation (everything found under the docs/ subdirectory in
# the source tree) is licensed under the Creative Commons CC BY 3.0
# license, which can be found at:
#
#          https://creativecommons.org/licenses/by/3.0/
#
''')
            yaml.dump(cases, f, width=2**32)

    emit_tests('[1.5, 1.5]', load_jq_tests_from_url('https://raw.githubusercontent.com/stedolan/jq/jq-1.5/tests/jq.test'), 'tests/jq-1.5.yaml')
    emit_tests('[1.5, 1.5]', load_jq_tests_from_url('https://raw.githubusercontent.com/stedolan/jq/jq-1.5/tests/onig.test'), 'tests/jq-1.5-onig.yaml')
    emit_tests('[1.5, 1.5]', load_jq_manual('https://raw.githubusercontent.com/stedolan/jq/master/docs/content/3.manual/v1.5/manual.yml'), 'tests/jq-1.5-manual.yaml')

    emit_tests('[1.6, 1.6]', load_jq_tests_from_url('https://raw.githubusercontent.com/stedolan/jq/master/tests/jq.test'), 'tests/jq-1.6.yaml')
    emit_tests('[1.6, 1.6]', load_jq_tests_from_url('https://raw.githubusercontent.com/stedolan/jq/master/tests/onig.test'), 'tests/jq-1.6-onig.yaml')
    emit_tests('[1.6, 1.6]', load_jq_manual('https://raw.githubusercontent.com/stedolan/jq/master/docs/content/3.manual/manual.yml'), 'tests/jq-1.6-manual.yaml')

