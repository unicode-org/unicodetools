from collections import defaultdict
from typing import Callable

symbols : list[str] = []
with open("unicodetools/data/pri555/18.0.0/LineBreakSymbols.txt") as f:
  for line in f.readlines():
    line = line.split("#")[0].strip()
    if not line:
      continue
    id, definition, non_dictionary_equivalent = (field.strip() for field in line.split(";"))
    symbols.append(id)
transitions : dict[str, dict[str, str]] = defaultdict(dict)
with open("unicodetools/data/pri555/18.0.0/LineBreakTransitions.txt") as f:
  for line in f.readlines():
    line = line.split("#")[0].strip()
    if not line:
      continue
    start, ahead, end = (field.strip() for field in line.split(";"))
    transitions[start][ahead] = end
if set(ahead for t in transitions.values() for ahead in t) != set(symbols):
  raise ValueError(set(ahead for t in transitions.values() for ahead in t) - set(symbols),
                   set(symbols) - set(ahead for t in transitions.values() for ahead in t))
accepting : dict[str, str] = {}
lookahead : dict[str, str] = {}
with open("unicodetools/data/pri555/18.0.0/LineBreakStates.txt") as f:
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
  return tuple(Π_index(transitions[state].get(c)) for c in symbols)
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

possible_lookaheads : dict[str, set[str]] = defaultdict(set)

for l in lookaheads:
  for source in states:
    if lookahead.get(source) != l:
      continue
    boundary = [(state, [source, symbol])
                for symbol, state
                in transitions[source].items()
                if accepting[state] != l]
    visited : set[str] = set()
    while boundary:
      s, path = boundary.pop()
      possible_lookaheads[s].add(l)
      visited.add(s)
      for symbol, t in transitions[s].items():
        if t not in visited and accepting[state] != l:
          boundary.append((t, path + [symbol]))

def old_unsafety_reason(c1 : str, c2 : str):
  expected_s1 = None
  expected = None
  result = set()
  for s1 in states:
    s2 = transitions[s1].get(c1)
    if not s2:
      end = None
    else:
      end = transitions[s2].get(c2)
    if not expected:
      expected = end
      expected_s1 = s1
    if end != expected:
      result.add(f"{c1}, {c2}: {s1} -> {end}; {expected_s1} -> {expected}")
  return result

def step_twice(l : str|None, s1 : str, c1 : str, c2 : str):
  last_break = -1
  injection = True
  while True:
    lookahead_positions : dict[str, int] = {}
    if injection:
      injection = False
      s = s1
      last_accepting_position = -1
      i = 0
      if l:
        lookahead_positions[l] = -1

      if accepting[s] == "Yes":
        last_accepting_position = i
      elif accepting[s] in lookahead_positions:
        # If l was set earlier and s accepts it, we break before position 0, and
        # we will come back to (c1, c2) in a different configuration.
        return None
      if s in lookahead:
        # If this lookahead at position 0 is accepted, we will go back through
        # (c1, c2) in the start state.
        lookahead_positions[lookahead[s]] = i
    elif last_break == -1:
      # With lookahead l set earlier, on state s, (c1, c2) finds a break
      # before position 0, so we will get back to (c1, c2) in a different
      # configuration.
      return None
    elif last_break == 0:
      # Optimization, probably pointless:
      # This will be covered by l=None, s1="START".
      return None
    else:
      s = "START"
      i = last_break
    text = (c1, c2)
    while True:
      if i == 2:
        return s
      ahead = text[i]
      i += 1
      if ahead in transitions[s]:
        s = transitions[s][ahead]
      else:
        last_break = last_accepting_position
        break

      if accepting[s] == "Yes":
        last_accepting_position = i
      elif accepting[s] in lookahead_positions:
        last_break = lookahead_positions[accepting[s]]
        break
      if s in lookahead:
        lookahead_positions[lookahead[s]] = i

def unsafety_reason(c1 : str, c2 : str):
  expected_s1 = None
  expected_l = None
  expected = None
  result = set()
  for s1 in states:
    for l in [None] + lookaheads:
      end = step_twice(l, s1, c1, c2)
      if not end:
        continue
      if not expected:
        expected_s1 = s1
        expected_l = l
        expected = end
      if end != expected:
        result.add(f"{c1}, {c2}: [{l}] {s1} -> {end}; "
                   f"[{expected_l}] {expected_s1} -> {expected}")
  return result

def mid_unsafety_reason(c1 : str, c2 : str):
  expected = step_twice(None, "START", "BK|NL|eot", c2)
  result = set()
  for s1 in states:
    for l in [None] + lookaheads:
      end = step_twice(l, s1, c1, c2)
      if not end:
        continue
      if end != expected:
        result.add(f"{c1}, {c2}: [{l}] {s1} -> {end}; "
                   f"START, {c2} -> {expected}")
  return result

old_safe_pairs = set()
safe_pairs = set()
mid_safe_pairs = set()

for c1 in symbols:
  for c2 in symbols:
    if not old_unsafety_reason(c1, c2):
      old_safe_pairs.add((c1, c2))
    if not unsafety_reason(c1, c2):
      safe_pairs.add((c1, c2))
    if not mid_unsafety_reason(c1, c2):
      mid_safe_pairs.add((c1, c2))

print("OLD:")
print(old_unsafety_reason("AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned", "SP"))
print(old_unsafety_reason("SP", "AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned"))
print(old_unsafety_reason("AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned", "AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned"))
print("NEW:")
print(unsafety_reason("AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned", "SP"))
print(unsafety_reason("SP", "AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned"))
print(unsafety_reason("AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned", "AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned"))
print("MID:")
print(mid_unsafety_reason("AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned", "SP"))
print(mid_unsafety_reason("SP", "AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned"))
print(mid_unsafety_reason("AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned", "AImEastAsian|ALmEastAsianmDottedCircle|SG|XXmExtPictUnassigned"))
print("Newly unsafe:", len(old_safe_pairs-safe_pairs))
print("Previously safe:", len(old_safe_pairs))
print("Newly safe:", len(safe_pairs-old_safe_pairs))
print("Total safe:", len(safe_pairs))
print("All pairs:", len(symbols) ** 2)
print("Mid safe:", len(mid_safe_pairs))
print("Safe but not mid safe:", len(safe_pairs - mid_safe_pairs), safe_pairs - mid_safe_pairs)