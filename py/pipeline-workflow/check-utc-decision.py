
import os
import json
import re

with open(os.environ['GITHUB_EVENT_PATH'], 'r') as f:
    event = json.load(f)
print(event)
pr_body = event['pull_request']['body']
if not re.search(r"UTC-\d\d\d-[MC]\d", pr_body):
    print("::error title=Need UTC decision::PRs for approved or provisionally assigned must include a link to a UTC decision.")
    exit(1)
