// Lemma to snippet to position of the word in the snippet.
/**@type {Map<string, Map<String, number>>}*/
let wordIndex/*= GENERATED LINE*/;
// Property name to snippet to index entry.
/**@type {Map<string, Map<string, {html: string, characters: [number, number][]}>>}*/
let indexEntries/*= GENERATED LINE*/;

/**@type {Map<number, string>}*/
let characterNames = new Map();
/**@type {Map<[number, number], string>}*/
let characterNameRanges = new Map();
for (let [name, entry] of indexEntries.get("Name")) {
  if (entry.characters[0][0] == entry.characters[0][1]) {
    characterNames.set(entry.characters[0][0], name);
  } else {
    for (let range of entry.characters) {
      characterNameRanges.set(range, name);
    }
  }
}
for (let [name, entry] of indexEntries.get("Name_Alias")) {
  if (!characterNames.has(entry.characters[0][0])) {
    characterNames.set(entry.characters[0][0], name);
  }
}

function updateResults(event) {
  /**@type {string}*/
  let query = event.target.value;
  let resultEntries = search(query);
  if (resultEntries.length >= 100) {
    document.getElementById("info").innerHTML = "Showing first 100 results";
  } else {
    document.getElementById("info").innerHTML = resultEntries.length + " results";
  }
  document.getElementById("results").innerHTML = "<tr><td>" + resultEntries.join("</tr></tr><tr><td>") + "</td></tr>";
}

function search(/**@type {string}*/ query) {
  let wordBreak = new Intl.Segmenter("en", { granularity: "word" });
  let queryWords = Array.from(wordBreak.segment(query)).filter(s => s.isWordLike).map(s => s.segment);
  let foldedQuery = queryWords.map(fold);
  var covered = [];
  /**@type {string[]}*/
  var result = [];
  /**@type {Set<string>}*/
  var resultSnippets = new Set(wordIndex.get(foldedQuery[0])?.keys() ?? []);
  let firstLemmata = [foldedQuery[0]];
  if (resultSnippets.size === 0 && foldedQuery.length == 1) {
    let prefix = fold(queryWords.at(-1));
    for (let [completion, leaves] of wordIndex) {
      if (completion.startsWith(prefix)) {
        firstLemmata.push(completion);
        resultSnippets = resultSnippets.union(leaves);
      }
    }
  }
  for (var i = 1; i < foldedQuery.length; ++i) {
    var rhs = new Set(wordIndex.get(foldedQuery[i])?.keys() ?? []);
    let intersection = resultSnippets.intersection(rhs);
    if (intersection.size === 0 && i == foldedQuery.length - 1) {
      let prefix = fold(queryWords.at(-1));
      for (let [completion, leaves] of wordIndex) {
        if (completion.startsWith(prefix)) {
          rhs = rhs.union(leaves);
        }
      }
      resultSnippets = resultSnippets.intersection(rhs);
    } else {
      resultSnippets = intersection;
    }
  }
  let pivots = firstLemmata.map(l => wordIndex.get(l)).filter(x => !!x);
  let getPivot = (/**@type {string}*/s) => pivots.map(p => p.get(s)).filter(x => x !== undefined)[0];
  let collator = new Intl.Collator("en");
  resultSnippets = Array.from(resultSnippets).sort(
    (left, right) => collator.compare(
      left.substring(getPivot(left)) +
                      ' \uFFFE ' +
                      left.substring(0, getPivot(left)),
      right.substring(getPivot(right)) +
                      ' \uFFFE ' +
                      right.substring(0, getPivot(right))));
  for (let [property, propertyIndex] of indexEntries) {
    /**@type {[number, number][]}*/
    for (let snippet of resultSnippets) {
      let entry = propertyIndex.get(snippet);
      if (!entry) {
        continue;
      }
      let entrySet = entry.characters;
      if (superset(covered, entrySet)) {
        continue;
      }
      covered = covered.concat(entrySet);
      let pivot = getPivot(snippet);
      let tail = snippet.substring(pivot);
      result.push(entry.html.replace(
        "[RESULT TEXT]",
        "<span class=tail" +
        (snippet.includes(",") ? " style=width:100%" : "") + ">" +
        toHTML(tail) +
        (pivot > 0 && !tail.endsWith(".") ? "," : "") +
        "</span> " +
          (pivot > 0 ? "<span class=head>" +
                       toHTML(snippet.substring(0, pivot)) +
                       "</span>"
                     : "")));
      if (result.length >= 100) {
        return result;
      }
    }
  }
  if (queryWords.length == 1) {
    let codePoints = [];
    if (/^[0-9A-F]+$/ui.test(queryWords[0])) {
      codePoints.push(parseInt(queryWords[0], 16));
    }
    if (/^.$/ui.test(queryWords[0])) {
      codePoints.push(queryWords[0].codePointAt(0));
    }
    for (let cp of codePoints) {
      var name = characterNames.get(cp);
      if (!name) {
        for (let [[first, last], n] of characterNameRanges) {
          if (first <= cp && cp <= last) {
            name = n;
            break;
          }
        }
      }
      if (name) {
        result.push(
          (indexEntries.get("Name").get(name) ??
          indexEntries.get("Name_Alias").get(name)).html.replace(
          "[RESULT TEXT]", toHTML(name)));
      }
    }
  } else if (queryWords.length == 1 && /^boop$/i.test(queryWords[0])) {
      result.push(
        indexEntries.get("Block").get("Betty").html.replace(
        "[RESULT TEXT]", toHTML("Betty")));
  } else if (queryWords.length == 1 && /^dood$/i.test(queryWords[0])) {
      result.push(
        indexEntries.get("Block").get("the").html.replace(
        "[RESULT TEXT]", toHTML("the")));
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

function fold(/**@type {string}*/ word) {
  let folding = word.toLowerCase();
  return folding.replace("š", "sh");
}