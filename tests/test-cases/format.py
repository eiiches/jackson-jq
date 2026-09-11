#!/usr/bin/env python3
import glob
import json
import os
import sys
import yaml
import yaml.nodes


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
            "q", "in", "out", "v", "failing", "comment", "justification",
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


def extract_header_comment(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        lines = f.readlines()
    header = []
    for line in lines:
        if line.strip().startswith("#") or (not line.strip() and header):
            header.append(line)
        elif not line.strip():
            continue
        else:
            break
    header_text = "".join(header)
    # Only retain header if it is a license block
    if "Permission is hereby granted" in header_text or "Creative Commons" in header_text:
        return header_text
    return ""


def format_file(filepath):
    header = ""
    if filepath.endswith(".yaml") or filepath.endswith(".yml"):
        header = extract_header_comment(filepath)
        with open(filepath, "r", encoding="utf-8") as fp:
            data = yaml.safe_load(fp)
    elif filepath.endswith(".json"):
        with open(filepath, "r", encoding="utf-8") as fp:
            data = json.load(fp)
    else:
        return

    if not isinstance(data, list):
        print(f"Skipping {filepath}: top-level element is not a list")
        return

    parts = [
        yaml.dump([FormattedTestCase(c)], width=2**32, allow_unicode=False).strip()
        for c in data
    ]
    yaml_text = "\n\n".join(parts) + "\n" if parts else "[]\n"

    target_path = filepath
    if filepath.endswith(".json"):
        target_path = filepath[:-5] + ".yaml"

    with open(target_path, "w", encoding="utf-8") as fp:
        if header:
            fp.write(header)
            if not header.endswith("\n"):
                fp.write("\n")
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
