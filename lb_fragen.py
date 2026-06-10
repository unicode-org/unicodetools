from collections import defaultdict
from typing import Callable

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

print(len(states), "states")
lookaheads = sorted(set(lookahead.values()))
print(len(lookaheads), "lookaheads")

def is_reachable(source  : str,
                 is_sink : Callable[[str], bool],
                 exclude : Callable[[str], bool]) -> bool:
  # Not [source]; if source sets l and accepts k, we need a source-to-source
  # path for reachability.
  boundary = [(state, [source, symbol])
              for symbol, state
              in transitions[source].items()
              if not exclude(state)]
  visited : set[str] = set()
  while boundary:
    s, path = boundary.pop()
    if is_sink(s):
      print("By " + " ".join(path) + ",")
      return True
    visited.add(s)
    for symbol, t in transitions[s].items():
      if t not in visited and not exclude(t):
        boundary.append((t, path + [symbol]))
  return False

reachability : set[tuple[str, str]] = set()

for l in lookaheads:
  for k in lookaheads:
    if k == l:
      continue
    for source in states:
      if lookahead.get(source) != l:
        continue
      if is_reachable(source,
                   lambda s: accepting[s] == k,
                   exclude=lambda s: lookahead.get(s) == k):
        print(k, "reachable from", l)
        reachability.add((k, l))
        break

def lookahead_colouring():
  χ = 0
  while True:
    χ += 1
    colours = [0] * len(lookaheads)
    while True:
      lookahead_colours = {lookaheads[i] : colours[i] for i in range(len(lookaheads))}
      for source, sink in reachability:
        if lookahead_colours[source] == lookahead_colours[sink]:
          break
      else:
        print(f"lookaheads are {χ}-colourable")
        print([[l for l in lookaheads if lookahead_colours[l] == c] for c in range(χ)])
        return lookahead_colours
      colours[0] += 1
      i = 0
      while i < len(colours) - 1 and colours[i] == χ:
        colours[i] = 0
        colours[i + 1] += 1
        i += 1
      if colours[-1] == χ:
        break
    print(f"lookaheads are not {χ}-colourable")

lookahead_colours = lookahead_colouring()

# Dragon book algorithm 3.6 & figure 3.45, starting with a partition by
# lookahead-aware type rather than just accepting or not.

states_by_type : dict[tuple[int|bool, int|None], set[str]] = defaultdict(set)
for state in states:
  states_by_type[False if accepting[state] == "No" else
                 True if accepting[state] == "Yes" else
                 lookahead_colours[accepting[state]],
                 lookahead_colours.get(lookahead.get(state))].add(state)
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

print(len(Π), "parts after minimization")
print("total", sum(len(g) for g in Π))
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
    
