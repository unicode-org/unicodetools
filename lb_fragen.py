from collections import defaultdict

transitions : dict[str, dict[str, str]] = defaultdict(dict)
with open("LineBreakTransitions.txt") as f:
  for line in f.readlines():
    line = line.split("#")[0].strip()
    if not line:
      continue
    start, ahead, end = (field.strip() for field in line.split(";"))
    transitions[start][ahead] = end

states = list(transitions.keys())

zwj_states = set(state for state in states if state.endswith("ZWJ"))
print(len(zwj_states), "ZWJ states")
for state in states:
  for ahead, end in transitions[state].items():
    if ahead != "ZWJ" and end in zwj_states:
      raise ValueError(state, ahead, end)
states_through_zwj = set(transitions[start].get("ZWJ") for start in states)
if zwj_states - states_through_zwj:
  raise ValueError(zwj_states - states_through_zwj)

for state in zwj_states:
  base = state.removesuffix(" ZWJ")
  for ahead, end in transitions[state].items():
    if (transitions[base].get(ahead) or transitions["START"][ahead]) != end:
      raise ValueError((state, ahead, end), (base, ahead, (transitions[base].get(ahead) or transitions["START"][ahead])))
  for ahead, end in transitions[base].items():
    if transitions[state].get(ahead) != end:
      raise ValueError((state, ahead, transitions[state].get(ahead)), (base, ahead, end))