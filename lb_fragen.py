from collections import defaultdict

classes : list[str] = []
with open("LineBreakClasses.txt") as f:
  for line in f.readlines():
    line = line.split("#")[0].strip()
    if not line:
      continue
    id, definition = (field.strip() for field in line.split(";"))
    classes.append(id)
classes.append("eot")
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
    state, a, l = (field.strip() for field in line.split(";"))
    accepting[state] = a
    if l:
      lookahead[state] = l

states = list(accepting.keys())
if lookahead.keys() - set(states):
  raise ValueError()
if transitions.keys() - set(states):
  raise ValueError()

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

minimized_states = set(minimizer.values())
minimized_transitions : dict[str, dict[str, str]] = defaultdict(dict)
for state, t in transitions.items():
  for ahead, end in t.items():
    if ahead in minimized_transitions[minimizer[state]]:
      if minimized_transitions[minimizer[state]][ahead] != minimizer[end]:
        raise ValueError(minimizer[state], ahead,
                         (minimized_transitions[minimizer[state]][ahead],
                          minimizer[end]))
    else:
      minimized_transitions[minimizer[state]][ahead] = minimizer[end]


print("Difference between AK and AK AK:")
for c in classes:
  if minimized_transitions["AK"].get(c) != minimized_transitions["AK AK"].get(c):
    print("On", c, minimized_transitions["AK"].get(c), "vs.",
          minimized_transitions["AK AK"].get(c))

zwj_states = set(state for state in minimized_states if state.endswith("ZWJ"))
print(len(zwj_states), "ZWJ states")
for state in states:
  for ahead, end in minimized_transitions[state].items():
    if ahead != "ZWJ" and end in zwj_states:
      raise ValueError(state, ahead, end)
states_through_zwj = set(minimized_transitions[start].get("ZWJ") for start in states)
if zwj_states - states_through_zwj:
  raise ValueError(zwj_states - states_through_zwj)

easy_zwj_states = set(s for s in zwj_states if s.removesuffix(" ZWJ") not in lookahead)

for state in easy_zwj_states:
  base = state.removesuffix(" ZWJ")
  for ahead, end in minimized_transitions[state].items():
    if (minimized_transitions[base].get(ahead) or minimized_transitions["START"][ahead]) != end:
      raise ValueError((state, ahead, end), (base, ahead, (minimized_transitions[base].get(ahead) or minimized_transitions["START"][ahead])))
  for ahead, end in minimized_transitions[base].items():
    if minimized_transitions[state].get(ahead) != end:
      raise ValueError((state, ahead, minimized_transitions[state].get(ahead)), (base, ahead, end))
    
