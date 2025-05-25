/**@type {Map<string, Map<string, Set<String>>>}*/
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
  let emptySet = new Set([]);
  for (let [property, wordIndex] of wordIndices) {
    let propertyLeaves = leaves.get(property);
    /**@type {Set<string>}*/
    var resultLeaves = new Set(wordIndex.get(queryWords[0]) ?? []);
    for (var i = 1; i < queryWords.length; ++i) {
      resultLeaves = resultLeaves.intersection(wordIndex.get(queryWords[i]) ?? emptySet);
    }
    /**@type {[number, number][]}*/
    for (let leaf of resultLeaves) {
      let entry = propertyLeaves.get(leaf);
      let leafSet = entry.characters;
      if (superset(covered, leafSet)) {
        continue;
      }
      covered = covered.concat(leafSet);
      result.push(entry.html);
      if (result.length >= 100) {
        return result;
      }
    }
  }
  return result;
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