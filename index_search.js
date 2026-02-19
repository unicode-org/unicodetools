// Lemma to property value to position of the word in the property value.
/**@type {Map<string, Map<String, number>>}*/
let wordIndex/*= GENERATED LINE*/;
// Property name to property value to index entry.
/**@type {Map<string, Map<string, {html: string, characters: [number, number][]}>>}*/
let leaves/*= GENERATED LINE*/;

/**@type {Map<number, string>}*/
let nameLeaves = new Map();
for (let [name, leaf] of leaves.get("Name")) {
  nameLeaves.set(leaf.characters[0][0], name);
}
for (let [name, leaf] of leaves.get("Name_Alias")) {
  if (!nameLeaves.has(leaf.characters[0][0])) {
    nameLeaves.set(leaf.characters[0][0], name);
  }
}

function updateResults(event) {
  /**@type {string}*/
  let query = event.target.value;
  let leaves = search(query);
  if (leaves.length >= 100) {
    document.getElementById("info").innerHTML = "Showing first 100 results";
  } else {
    document.getElementById("info").innerHTML = leaves.length + " results";
  }
  document.getElementById("results").innerHTML = "<tr><td>" + leaves.join("</tr></tr><tr><td>") + "</td></tr>";
}
function search(/**@type {string}*/ query) {
  let wordBreak = new Intl.Segmenter("en", { granularity: "word" });
  let queryWords = Array.from(wordBreak.segment(query)).filter(s => s.isWordLike).map(s => s.segment);
  let queryLemmata = queryWords.map(lemmatize);
  var covered = [];
  /**@type {string[]}*/
  var result = [];
  /**@type {Set<string>}*/
  var resultLeaves = new Set(wordIndex.get(queryLemmata[0])?.keys() ?? []);
  let firstLemmata = [queryLemmata[0]];
  if (resultLeaves.size === 0 && queryLemmata.length == 1) {
    let prefix = queryWords.at(-1);
    for (let [completion, leaves] of wordIndex) {
      if (completion.startsWith(prefix)) {
        firstLemmata.push(completion);
        resultLeaves = resultLeaves.union(leaves);
      }
    }
  }
  for (var i = 1; i < queryLemmata.length; ++i) {
    var rhs = new Set(wordIndex.get(queryLemmata[i])?.keys() ?? []);
    let intersection = resultLeaves.intersection(rhs);
    if (intersection.size === 0 && i == queryLemmata.length - 1) {
      let prefix = queryWords.at(-1);
      for (let [completion, leaves] of wordIndex) {
        if (completion.startsWith(prefix)) {
          rhs = rhs.union(leaves);
        }
      }
      resultLeaves = resultLeaves.intersection(rhs);
    } else {
      resultLeaves = intersection;
    }
  }
  let pivots = firstLemmata.map(l => wordIndex.get(l)).filter(x => !!x);
  let getPivot = (/**@type {string}*/s) => pivots.map(p => p.get(s)).filter(x => x !== undefined)[0];
  let collator = new Intl.Collator("en");
  resultLeaves = Array.from(resultLeaves).sort(
    (left, right) => collator.compare(
      left.substring(getPivot(left)) +
                      ' \uFFFE ' +
                      left.substring(0, getPivot(left)),
      right.substring(getPivot(right)) +
                      ' \uFFFE ' +
                      right.substring(0, getPivot(right))));
  for (let [property, propertyLeaves] of leaves) {
    /**@type {[number, number][]}*/
    for (let leaf of resultLeaves) {
      let entry = propertyLeaves.get(leaf);
      if (!entry) {
        continue;
      }
      let leafSet = entry.characters;
      if (superset(covered, leafSet)) {
        continue;
      }
      covered = covered.concat(leafSet);
      let pivot = getPivot(leaf);
      let tail = leaf.substring(pivot);
      result.push(entry.html.replace(
        "[RESULT TEXT]",
        "<span class=tail" +
        (leaf.includes(",") ? " style=width:100%" : "") + ">" +
        toHTML(tail) +
        (pivot > 0 && !tail.endsWith(".") ? "," : "") +
        "</span> " +
          (pivot > 0 ? "<span class=head>" +
                       toHTML(leaf.substring(0, pivot)) +
                       "</span>"
                     : "")));
      if (result.length >= 100) {
        return result;
      }
    }
  }
  if (queryWords.length == 1 && /^[0-9A-F]+$/i.test(queryWords[0])) {
    let name = nameLeaves.get(parseInt(queryWords[0], 16));
    if (name) {
      result.push(
        (leaves.get("Name").get(name) ??
         leaves.get("Name_Alias").get(name)).html.replace(
        "[RESULT TEXT]", toHTML(name)));
    }
  }
  return result;
}

function toHTML(/**@type {string}*/ plain) {
  return plain.replaceAll("&", "&amp;")
              .replaceAll("<", "&lt;")
              .replaceAll(">", "&gt;")
}

function superset(/**@type {[number, number][]}*/left, /**@type {[number, number][]}*/right) {
  var remaining = right.slice();
  for (containingRange of left) {
    remaining = remaining.flatMap(r => rangeMinus(r, containingRange));
  }
  if (remaining.length > 0) {
    return false;
  }
  return true;
}

function rangeMinus(/**@type {[number, number]}*/left, /**@type {[number, number]}*/right) {
  let intersection = rangeIntersection(left, right);
  if (intersection === left || intersection === right) {
    return [];
  } else if (intersection === null) {
    return [left];
  } {
    /**@type {[number, number][]}*/
    let result = [];
    if (left[0] < intersection[0]) {
      result.push([left[0], intersection[0] - 1]);
    }
    if (left[1] > intersection[1]) {
      result.push([intersection[1] + 1, left[1] - 1]);
    }
    return result;
  }
}

function rangeIntersection(/**@type {[number, number]}*/left, /**@type {[number, number]}*/right) {
  let [leftStart, leftEnd] = left;
  let [rightStart, rightEnd] = right;
  if (leftEnd < rightStart || rightEnd < leftStart) {
    return null;
  } else {
    return [Math.max(leftStart, rightStart), Math.min(leftEnd, rightEnd)];
  }
}

function lemmatize(/**@type {string}*/ word) {
  let lemma = word.toLowerCase();
  lemma = lemma.replace("š", "sh");
  if (lemma.endsWith("ses") && lemma.length > 4) {
      lemma = lemma.substring(0, lemma.length - 2);
  } else if (lemma.endsWith("s") && !lemma.endsWith("ss") && lemma.length > 2) {
      lemma = lemma.substring(0, lemma.length - 1);
  }
  return lemma;
}