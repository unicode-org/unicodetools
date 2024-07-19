
import os
import json
import re

with open(os.environ['GITHUB_EVENT_PATH'], 'r') as f:
    event = json.load(f)
print(event)
pr_body = event['pull_request']['body']
errors = 0
if not re.search(r"L2/\d\d-\d\d\d", pr_body):
    print("::error title=Need proposal document::"
          "PRs for character additions must include a link to an L2 document in"
          " the PR description.")
    errors += 1
if not re.search(r"(unicode-org/sah(#|/issues/)\d|CJK|ESC)", pr_body):
    print("::error title=Need working group reference::"
          "PRs for character additions must include a link to the SAH issue, or "
          "the mention ESC or CJK.")
    errors += 1
if not re.search(r"(unicode-org/utc-release-management(#|/issues/)\d)", pr_body):
    print("::error title=Need RMG reference::"
          "PRs for character additions must include a link to the corresponding "
          "RMG issue.")
    errors += 1
exit(errors)
