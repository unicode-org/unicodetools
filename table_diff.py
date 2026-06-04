from collections import defaultdict
import sys

classes : list[str] = []
with open("LineBreakClasses.txt") as f:
  for line in f.readlines():
    line = line.split("#")[0].strip()
    if not line:
      continue
    id, definition = (field.strip() for field in line.split(";"))
    classes.append(id)
transitions : dict[str, dict[str, str]] = defaultdict(dict)
with open("LineBreakTransitions.txt") as f:
  for line in f.readlines():
    line = line.split("#")[0].strip()
    if not line:
      continue
    start, ahead, end = (field.strip() for field in line.split(";"))
    transitions[start][ahead] = end
if set(ahead for t in transitions.values() for ahead in t) != set(classes):
  raise ValueError(set(ahead for t in transitions.values() for ahead in t) - set(classes),
                   set(classes) - set(ahead for t in transitions.values() for ahead in t))
accepting : dict[str, str] = {}
lookahead : dict[str, str] = {}
with open("LineBreakStates.txt") as f:
  for line in f.readlines():
    line = line.split("#")[0].strip()
    if not line:
      continue
    state, a, l, status = (field.strip() for field in line.split(";"))
    accepting[state] = a
    if l:
      lookahead[state] = l

states = list(accepting.keys())
if lookahead.keys() - set(states):
  raise ValueError()
if transitions.keys() - set(states):
  raise ValueError()

c1 = sys.argv[1]
c2 = sys.argv[2]

if c1 not in states:
  raise KeyError(c1)
if c2 not in states:
  raise KeyError(c2)

print(len(states), "states")

# Dragon book algorithm 3.6 & figure 3.45, starting with a partition by
# lookahead-aware type rather than just accepting or not.

states_by_type : dict[tuple[str, str|None], set[str]] = defaultdict(set)
for state in states:
  states_by_type[accepting[state], lookahead.get(state)].add(state)
Π = list(states_by_type.values())
def Π_index(state : str|None):
  if state is None:
    return None
  for i, g in enumerate(Π):
    if state in g:
      return i
def Π_signature(state : str):
  return tuple(Π_index(transitions[state].get(c)) for c in classes)
while True:
  for g in Π:
    subgroups : dict[str, set[str]] = defaultdict(set)
    if c1 in g and c2 in g and Π_signature(c1) != Π_signature(c2):
      print("Distinguishing", c1, "from", c2)
      for c in classes:
        if Π_index(transitions[c1].get(c)) != Π_index(transitions[c2].get(c)):
          print(c1, "on", c, "to", [s for s in states if Π_index(s) == Π_index(transitions[c1].get(c))])
          print(c2, "on", c, "to", [s for s in states if Π_index(s) == Π_index(transitions[c2].get(c))])
    for s in g:
      subgroups[Π_signature(s)].add(s)
    if len(subgroups) > 1:
      print("refining group of size", len(g), "into subgroups of sizes",
            [len(sg) for sg in subgroups.values()])
      Π.remove(g)
      for sg in subgroups.values():
        Π.append(sg)
      break
  else:
    break

print(len(Π), "parts total is", sum(len(g) for g in Π))
minimizer : dict[str, str] = {}
for g in Π:
  sorted_group = sorted(g, key=lambda s: (len(s.split()), len(s), s))
  for s in sorted_group:
    minimizer[s] = sorted_group[0]
  print(sorted_group)
