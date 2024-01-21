
import os
import json

pipeline_label = os.environ['PIPELINE_LABEL']

with open("unicodetools/data/ucd/dev/DerivedAge.txt", 'r') as f:
    version = f.readline().strip().replace("# DerivedAge-", "").replace(".0.txt", "")

PROVISIONALLY_ASSIGNED_AGE_NOTICE = f"""\f
While the Unicode Technical Committee has provisionally assigned these
characters, they have not been accepted for Unicode {version}, nor for any
specific version of Unicode.

The values of the Age property in this file are a placeholder.
""".replace("\n", "%0A")

UNREVIEWED_AGE_NOTICE = f"""\f
These characters are neither accepted for Unicode {version}, nor for any
specific version of Unicode, nor are they provisionally assigned.

The values of the Age property in this file are a placeholder.
""".replace("\n", "%0A")

if pipeline_label != "pipeline-" + version:
    with open(os.environ['GITHUB_EVENT_PATH'], 'r') as f:
        event = json.load(f)
    print(event)
    draft = event['pull_request']['draft']
    # Caution the reader that the Age values are placeholders.
    if pipeline_label == "pipeline-provisionally-assigned":
        print("::warning file=unicodetools/data/ucd/dev/DerivedAge.txt,"
              f"title=Not in the {version} pipeline::" +
              PROVISIONALLY_ASSIGNED_AGE_NOTICE)
    else:  # Not even provisionally assigned.
        print("::warning file=unicodetools/data/ucd/dev/DerivedAge.txt,"
              f"title=Not in the {version} pipeline::" +
              UNREVIEWED_AGE_NOTICE)
    if not draft:
        print("::error title=PR must be draft::"
              "PRs for character additions must be draft unless approved for "
              "the upcoming version of Unicode.")
        exit(1)
