
import os
import json
import re

with open(os.environ['GITHUB_EVENT_PATH'], 'r') as f:
    event = json.load(f)
print(event)
pr_body = event['pull_request']['body']
if not re.search(r"L2/\d\d-\d\d\d", pr_body):
    print("::error title=Need proposal document::PRs for character additions must include a link to an L2 document in the PR description.")
    exit(1)