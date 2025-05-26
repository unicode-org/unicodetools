/**@type {Map<string, Map<string, Map<String, number>>>}*/
let wordIndices/*= GENERATED LINE*/;
/**@type {Map<string, Map<string, {characters: [number, number][]}>>}*/
let leaves/*= GENERATED LINE*/;

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
  let queryWords = Array.from(wordBreak.segment(query)).filter(s => s.isWordLike).map(s => lemmatize(s.segment));
  var covered = [];
  /**@type {string[]}*/
  var result = [];
  for (let [property, wordIndex] of wordIndices) {
    let propertyLeaves = leaves.get(property);
    /**@type {Set<string>}*/
    var resultLeaves = new Set(wordIndex.get(queryWords[0])?.keys() ?? []);
    for (var i = 1; i < queryWords.length; ++i) {
      resultLeaves = resultLeaves.intersection(
          new Set(wordIndex.get(queryWords[i])?.keys() ?? []));
    }
    let pivots = wordIndex.get(queryWords[0]) || new Map([]);
    let collator = new Intl.Collator("en");
    resultLeaves = Array.from(resultLeaves).sort(
      (left, right) => collator.compare(
        left.substring(pivots.get(left)) +
                       ' \uFFFE ' +
                       left.substring(0, pivots.get(left)),
        right.substring(pivots.get(right)) +
                        ' \uFFFE ' +
                        right.substring(0, pivots.get(right))));
    /**@type {[number, number][]}*/
    for (let leaf of resultLeaves) {
      let entry = propertyLeaves.get(leaf);
      let leafSet = entry.characters;
      if (superset(covered, leafSet)) {
        continue;
      }
      covered = covered.concat(leafSet);
      let pivot = pivots.get(leaf);
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