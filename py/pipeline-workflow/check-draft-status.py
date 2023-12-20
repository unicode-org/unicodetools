
import os
import json

pipeline_label = os.environ['PIPELINE_LABEL']

with open("unicodetools/data/ucd/dev/DerivedAge.txt", 'r') as f:
    version = f.readline().strip().replace("# DerivedAge-", "").replace(".0.txt", "")

if pipeline_label != "pipeline-" + version:
    with open(os.environ['GITHUB_EVENT_PATH'], 'r') as f:
        event = json.load(f)
    print(event)
    draft = event['pull_request']['draft']
    if not draft:
        print("::error title=PR must be draft::PRs for character additions must be draft unless approved for the upcoming version of Unicode.")
        exit(1)
