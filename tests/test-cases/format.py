#!/usr/bin/env python3
import glob
import json
import os
import re
import sys
import yaml
import yaml.nodes


yaml.SafeLoader.add_implicit_resolver(
    "tag:yaml.org,2002:float",
    re.compile(
        r"""^(?:[-+]?(?:[0-9][0-9_]*)\.[0-9_]*(?:[eE][-+]?[0-9]+)?
|[-+]?(?:[0-9][0-9_]*)(?:[eE][-+]?[0-9]+)
|\.[0-9_]+(?:[eE][-+]?[0-9]+)?
|[-+]?[0-9][0-9_]*(?::[0-5]?[0-9])+\.[0-9_]*
|[-+]?\.(?:inf|Inf|INF)
|\.(?:nan|NaN|NAN))$""",
        re.VERBOSE,
    ),
    list("-+0123456789."),
)


def represent_json(dumper, value):
    if value is None:
        return dumper.represent_none(value)
    if isinstance(value, list):
        node = yaml.nodes.SequenceNode("tag:yaml.org,2002:seq", [], flow_style=True)
        for element in value:
            node.value.append(represent_json(dumper, element))
        return node
    if isinstance(value, dict):
        node = yaml.nodes.MappingNode("tag:yaml.org,2002:map", [], flow_style=True)
        for k, v in value.items():
            node.value.append((represent_json(dumper, k), represent_json(dumper, v)))
        return node
    if isinstance(value, str):
        return yaml.nodes.ScalarNode("tag:yaml.org,2002:str", value, style='"')
    if isinstance(value, (int, float, bool)):
        node = dumper.represent_data(value)
        node.style = None
        return node
    raise ValueError(f"unsupported type: {type(value)}")


class FormattedTestCase:
    def __init__(self, data):
        self.data = data

    def __yaml__(self, dumper):
        root_node = yaml.nodes.MappingNode("tag:yaml.org,2002:map", [], flow_style=False)
        known_order = [
            "q", "in", "out", "types", "properties", "v", "failing", "comment", "justification",
            "modules", "should_compile",
            "numerical_errors", "ignore_true_jq_behavior",
        ]
        keys = [k for k in known_order if k in self.data]
        for k in self.data:
            if k not in keys:
                keys.append(k)

        for k in keys:
            val = self.data[k]
            if k == "q":
                if isinstance(val, str) and "\n" in val:
                    val = " ".join(line.strip() for line in val.splitlines() if line.strip())
                q_node = dumper.represent_data(val)
                q_node.style = "'"
                root_node.value.append((dumper.represent_str("q"), q_node))
            elif k == "in":
                root_node.value.append((dumper.represent_str("in"), represent_json(dumper, val)))
            elif k == "out":
                if not isinstance(val, list):
                    val = [val]
                if len(val) == 0:
                    out_node = yaml.nodes.SequenceNode("tag:yaml.org,2002:seq", [], flow_style=True)
                else:
                    out_node = yaml.nodes.SequenceNode("tag:yaml.org,2002:seq", [], flow_style=False)
                    for o in val:
                        out_node.value.append(represent_json(dumper, o))
                root_node.value.append((dumper.represent_str("out"), out_node))
            elif k == "types":
                types_node = yaml.nodes.SequenceNode("tag:yaml.org,2002:seq", [], flow_style=False)
                for t in val:
                    item_node = yaml.nodes.MappingNode("tag:yaml.org,2002:map", [], flow_style=False)
                    in_node = yaml.nodes.ScalarNode("tag:yaml.org,2002:str", str(t["input"]), style="'")
                    out_node = yaml.nodes.ScalarNode("tag:yaml.org,2002:str", str(t["output"]), style="'")
                    item_node.value.append((dumper.represent_str("input"), in_node))
                    item_node.value.append((dumper.represent_str("output"), out_node))
                    types_node.value.append(item_node)
                root_node.value.append((dumper.represent_str("types"), types_node))
            elif k == "properties":
                props_node = yaml.nodes.MappingNode("tag:yaml.org,2002:map", [], flow_style=False)
                for pk in ["cardinality", "depends_on_input", "depends_on_external_state"]:
                    if pk in val:
                        pv = val[pk]
                        if pk == "cardinality":
                            pv_node = yaml.nodes.ScalarNode("tag:yaml.org,2002:str", str(pv), style=None)
                        elif isinstance(pv, bool):
                            pv_node = dumper.represent_data(pv)
                            pv_node.style = None
                        else:
                            pv_node = dumper.represent_data(pv)
                        props_node.value.append((dumper.represent_str(pk), pv_node))
                root_node.value.append((dumper.represent_str("properties"), props_node))
            elif k == "v":
                v_node = dumper.represent_data(val)
                v_node.style = "'"
                root_node.value.append((dumper.represent_str("v"), v_node))
            elif k == "comment":
                c_node = dumper.represent_data(val)
                c_node.style = "'"
                root_node.value.append((dumper.represent_str("comment"), c_node))
            elif k == "modules":
                modules_node = yaml.nodes.MappingNode("tag:yaml.org,2002:map", [], flow_style=False)
                for mk, mv in val.items():
                    mk_node = dumper.represent_str(mk)
                    if isinstance(mv, str):
                        mv_str = mv if mv.endswith("\n") else mv + "\n"
                        mv_node = yaml.nodes.ScalarNode("tag:yaml.org,2002:str", mv_str, style="|")
                    else:
                        mv_node = dumper.represent_data(mv)
                    modules_node.value.append((mk_node, mv_node))
                root_node.value.append((dumper.represent_str("modules"), modules_node))
            else:
                root_node.value.append((dumper.represent_str(k), dumper.represent_data(val)))
        return root_node


yaml.add_representer(FormattedTestCase, lambda dumper, data: data.__yaml__(dumper))


def extract_comments_and_data(content):
    lines = content.splitlines(keepends=True)
    tokens = [t for t in yaml.scan(content) if isinstance(t, yaml.BlockEntryToken) and t.start_mark.column == 0]
    data = yaml.load(content, Loader=yaml.SafeLoader)
    if not isinstance(data, list):
        return None, None, None

    if not tokens:
        all_comments = "".join(lines).strip()
        trailing_comment = (all_comments + "\n") if all_comments else ""
        return data, [], trailing_comment

    comments = []
    for i, t in enumerate(tokens):
        if i == 0:
            raw_comment_lines = lines[:t.start_mark.line]
        else:
            prev_start = tokens[i - 1].start_mark.line
            curr_start = t.start_mark.line
            raw_comment_lines = lines[prev_start:curr_start]
            last_item_line = -1
            for idx, l in enumerate(raw_comment_lines):
                if l.strip() and not l.startswith("#"):
                    last_item_line = idx
            raw_comment_lines = raw_comment_lines[last_item_line + 1:]

        while raw_comment_lines and not raw_comment_lines[0].strip():
            raw_comment_lines.pop(0)
        comments.append("".join(raw_comment_lines))

    last_start = tokens[-1].start_mark.line
    raw_trailing = lines[last_start:]
    last_item_line = -1
    for idx, l in enumerate(raw_trailing):
        if l.strip() and not l.startswith("#"):
            last_item_line = idx
    raw_trailing = raw_trailing[last_item_line + 1:]
    while raw_trailing and not raw_trailing[0].strip():
        raw_trailing.pop(0)
    trailing_comment = "".join(raw_trailing)

    return data, comments, trailing_comment


def format_file(filepath):
    if filepath.endswith(".yaml") or filepath.endswith(".yml"):
        with open(filepath, "r", encoding="utf-8") as fp:
            content = fp.read()
        data, comments, trailing_comment = extract_comments_and_data(content)
        if data is None or not isinstance(data, list):
            print(f"Skipping {filepath}: top-level element is not a list")
            return
    elif filepath.endswith(".json"):
        with open(filepath, "r", encoding="utf-8") as fp:
            data = json.load(fp)
        if not isinstance(data, list):
            print(f"Skipping {filepath}: top-level element is not a list")
            return
        comments = [""] * len(data)
        trailing_comment = ""
    else:
        return

    parts = []
    for c, comment in zip(data, comments):
        dumped = yaml.dump([FormattedTestCase(c)], width=2**32, allow_unicode=False).strip()
        if comment:
            parts.append(comment + dumped)
        else:
            parts.append(dumped)

    yaml_text = "\n\n".join(parts) + "\n" if parts else "[]\n"
    if trailing_comment:
        if not trailing_comment.endswith("\n"):
            trailing_comment += "\n"
        yaml_text += "\n" + trailing_comment

    target_path = filepath
    if filepath.endswith(".json"):
        target_path = filepath[:-5] + ".yaml"

    with open(target_path, "w", encoding="utf-8") as fp:
        fp.write(yaml_text)

    if filepath.endswith(".json") and target_path != filepath:
        os.remove(filepath)
        print(f"Converted {filepath} -> {target_path}")
    else:
        print(f"Formatted {filepath}")


def main():
    if len(sys.argv) > 1:
        files = sys.argv[1:]
    else:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        files = sorted(
            glob.glob(os.path.join(script_dir, "**/*.yaml"), recursive=True)
            + glob.glob(os.path.join(script_dir, "**/*.yml"), recursive=True)
            + glob.glob(os.path.join(script_dir, "**/*.json"), recursive=True)
        )
    for f in files:
        format_file(f)


if __name__ == "__main__":
    main()
