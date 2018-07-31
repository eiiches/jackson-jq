# coding: utf-8

import json
import yaml
from urllib import request as urllib2
import re
import html
import cgi

def htmlescape(string):
    def conv(match):
        return '&#' + str(ord(match.group())) + ';'
    pat = re.compile(r'[^0-9a-zA-Z]')
    return pat.sub(conv, string)

def tohash(string):
    return htmlescape(re.sub(r'[` ]', '', string))

def titleescape(string):
    chars = []

    inquote = False
    for ch in string:
        if ch == '`':
            inquote = not inquote
            chars.append('`')
            continue
        if inquote or 'a' <= ch <= 'z' or 'A' <= ch <= 'Z' or '0' <= ch <= '9' or ch == ' ' or ch == ',':
            if ch == '|':
                chars.append('ǀ')
            else:
                chars.append(ch)
        else:
            chars.append('&#' + str(ord(ch)) + ';')

    return ''.join(chars)

def foo():
    with urllib2.urlopen('https://raw.githubusercontent.com/stedolan/jq/master/docs/content/3.manual/v1.5/manual.yml') as f:
        manual = yaml.load(f)

    sections = []
    for section in manual['sections']:
        if section['title'] == 'Invoking jq':
            continue
        sections.append((1, section['title']))
        for subsection in section.get('entries', []):
            sections.append((2, subsection['title']))

    items = []
    for section in sections:
        bullet = '&nbsp;&nbsp;&nbsp;&nbsp;' * (section[0] - 1) + ('&bull; ' if section[0] > 1 else '')
        title = titleescape(section[1])
        items.append('{bullet}[{title}](https://stedolan.github.io/jq/manual/v1.5/#{hash})'.format(bullet=bullet, title=title, hash=tohash(section[1])))
    length = max(len(item) for item in items)

    print('| {head:<{width}} | jackson-jq |'.format(head='Feature', width=length))
    print('|-{head:<{width}}-|------------|'.format(head='-'*length, width=length))
    for item in items:
        print('| {item:<{width}} |            |'.format(item=item, width=length))

foo()
