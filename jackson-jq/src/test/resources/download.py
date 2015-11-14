# coding: utf-8

import json
import urllib2
from bs4 import BeautifulSoup

unsupported = [
    "env.PAGER",
    '[while(.<100; .*2|if . > 10 then break else . end)]',
    'del(.[2:4],.[0],.[-2:])',
    'def f: .+1; def g: f; def f: .+100; def f(a):a+.+11; [(g|f(20)), f]',
    'path(.foo[0,1])',
    'path(.[] | select(.>3))',
    'path(.)',
    '["foo",1] as $p | getpath($p), setpath($p; 20), delpaths([$p])',
    'map(getpath([2])), map(setpath([2]; 42)), map(delpaths([[2]]))',
    'map(delpaths([[0,"foo"]]))',
    'delpaths([[-200]])',
    'del(.), del(empty), del((.foo,.bar,.baz) | .[2,3,0]), del(.foo[0], .bar[0], .foo, .baz.bar[0].x)',
    'def inc(x): x |= .+1; inc(.[].a)',
    '[.[]|(.a, .a)?]',
    '[[.[]|[.a,.a]]?]',
    '[match("( )*"; "g")]',
    "bsearch(4)",
    "import \"a\" as foo; import \"b\" as bar; def fooa: foo::a; [fooa, bar::a, bar::b, foo::a]",
    "import \"c\" as foo; [foo::a, foo::c]",
    "modulemeta",
    "import \"test_bind_order\" as check; check::check",
    "[label | while(.<100; .*2|if . > 10 then break else . end)]",
    "[(label $here | .[] | if .>1 then break $here else . end), \"hi!\"]",
    "(label | (label | 2 | break2)), 1",
    "[label | foreach .[] as $item ([3, null]; if .[0] < 1 then break else [.[0] -1, $item] end; .[1])]",
    "[label $out | foreach .[] as $item ([3, null]; if .[0] < 1 then break $out else [.[0] -1, $item] end; .[1])]",
    "[foreach .[] as [$i, $j] (0; . + $i - $j)]",
    "[foreach .[] as {a:$a} (0; . + $a; -.)]",
    "[1, {c:3, d:4}] as [$a, {c:$b, b:$c}] | $a, $b, $c",
    ". as {as: $kw, \"str\": $str, (\"e\"+\"x\"+\"p\"): $exp} | [$kw, $str, $exp]",
    ".[] as [$a, $b] | [$b, $a]",
    ". as $i | . as [$i] | $i",
    ". as [$i] | . as $i | $i",
    "map_values(.+1)",
    "reduce .[] as [$i, {j:$j}] (0; . + $i - $j)",
    "reduce [[1,2,10], [3,4,10]][] as [$i,$j] (0; . + $i * $j)",
    "try error(\"\\($__loc__)\") catch .",
    "gsub(\"(?<d>\\\\d)\"; \":\(.d);\")",
    "[strptime(\"%Y-%m-%dT%H:%M:%SZ\")|(.,mktime)]",
    "strftime(\"%Y-%m-%dT%H:%M:%SZ\")",
    "gmtime",
    "{if:0,and:1,or:2,then:3,else:4,elif:5,end:6,as:7,def:8,reduce:9,foreach:10,try:11,catch:12,label:13,import:14,module:15}",
    "[1,null,Infinity,-Infinity,NaN,-NaN]",
]


def load_jq_all():
    result = []
    lines = urllib2.urlopen('https://raw.githubusercontent.com/stedolan/jq/master/tests/jq.test').read().split('\n')
    ite = iter(lines)
    while True:
        try:
            line = ite.next().decode('utf-8')
        except StopIteration:
            break
        if not line or line.startswith('#'):
            continue

        # if line.strip() == "%%FAIL":
        if line.strip().startswith("%%FAIL"):
            while line:
                try:
                    line = ite.next().decode('utf-8')
                except StopIteration:
                    break
            continue

        _q = line

        line = ite.next().decode('utf-8')
        print line
        try:
            _in = json.loads(line.replace(u'\ufeff', u' '))
        except ValueError:
            ite.next()

        _out = []
        while True:
            try:
                line = ite.next().decode('utf-8').replace('\ufeff', ' ')
            except StopIteration:
                break
            if not line or line.startswith('#'):
                break
            _out.append(json.loads(line))
        result.append({'q': _q, 'in': _in, 'out': _out})
    return result


def load_jq_manual():
    html = urllib2.urlopen('http://stedolan.github.io/jq/manual').read()
    soup = BeautifulSoup(html, 'lxml')

    def f(values):
        q = values[0].replace('jq ', '')[1:-1]

        # patch doc errors
        if q == 'rindex(", ")]':
            q = 'rindex(", ")'
        if q == 'unique(length)' and values[2] == '["chunky", "bacon", "asparagus"]':
            values[2] = '["bacon", "chunky", "asparagus"]'
        if q == 'recurse' and values[2] == '0':
            values.insert(2, '{"a":0,"b":[1]}')
        if q == '[.[]|endswith("foo")]' and values[2] == '[false, true, true, false, false]':
            values[2] = '[false,true]'
        if q == 'match("foo (?bar)? foo"; "ig")':
            q = 'match("foo (?<bar123>bar)? foo"; "ig")'
        if q == 'del(.foo)' and values[1] == '[{"foo": 42, "bar": 9001, "baz": 42}]':
            values[1] = '{"foo": 42, "bar": 9001, "baz": 42}'
        if q == 'del(.[1, 2])' and values[1] == '[["foo", "bar", "baz"]]':
            values[1] = '["foo", "bar", "baz"]'
        if q == '@html' and values[2] == '"This works if x &lt; y"':
            values[2] = '"This works if x &amp;lt; y"'

        def f_out(outs):
            if len(outs) == 1 and outs[0] == u'none':
                return []
            return map(lambda x: json.loads(x.replace('&lt;', '<').replace('&amp;', '&')), outs)

        try:
            return {'q': q, 'in': json.loads(values[1]), 'out': f_out(values[2:])}
        except Exception as e:
            print e, values

    d = map(f, [map(lambda td: td.text, elm.findAll('td')) for elm in soup.findAll('table', 'manual-example')])
    return d

if __name__ == '__main__':
    d = load_jq_all()
    with open('jq-test-all-ok.json', 'w') as f:
        json.dump(filter(lambda x: x['q'] not in unsupported, d), f, indent=4)
    with open('jq-test-all-ng.json', 'w') as f:
        json.dump(filter(lambda x: x['q'] in unsupported, d), f, indent=4)

    d = load_jq_manual()
    with open('jq-test-official-ok.json', 'w') as f:
        json.dump(filter(lambda x: x['q'] not in unsupported, d), f, indent=4)
    with open('jq-test-official-ng.json', 'w') as f:
        json.dump(filter(lambda x: x['q'] in unsupported, d), f, indent=4)
