
import os
import json

with open(os.environ['GITHUB_EVENT_PATH'], 'r') as f:
    event = json.load(f)
print(event)
labels = event['pull_request']['labels']
pipeline_labels = [label for label in labels if label['name'].startswith('pipeline-')]
if not pipeline_labels:
    print("::error title=Missing pipeline label::PRs for character additions must have a pipeline label.")
    exit(1)
if len(pipeline_labels) > 1:
    print("::error title=Multiple pipeline labels::Only one pipeline-* label must be applied.")
    exit(1)
label = pipeline_labels[0]
print("Labeled", label)
with open(os.environ['GITHUB_OUTPUT'], 'a') as f:
    print("pipeline-label=" + label['name'], file=f)