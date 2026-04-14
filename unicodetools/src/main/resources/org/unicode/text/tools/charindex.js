// Lemma to snippet (compressed) to position of the word in the snippet.
/**@type {Map<string, Map<number, number>>}*/
let wordIndex/*= GENERATED LINE*/;
// Property name to snippet (compressed) to index entry; the html is compressed.
/**@type {Map<string, Map<number, {html: number, characters: [number, number][]}>>}*/
let indexEntries/*= GENERATED LINE*/;
/**@type {number}*/
let bettyIndex/*= GENERATED LINE*/;
/**@type {number}*/
let theIndex/*= GENERATED LINE*/;
/**@type {string}*/
let allTheStringsCompressed/*= GENERATED LINE*/;
let decompressor = new DecompressionStream("deflate");
/**@type {string}*/
var allTheStrings;
new Response(
  new Blob([Uint8Array.fromBase64(allTheStringsCompressed)])
      .stream().pipeThrough(decompressor))
    .text().then(s => allTheStrings = s);

/**@type {Map<number, number>}*/
let characterNames = new Map();
/**@type {Map<[number, number], {property: string, snippetIndex: number}>}*/
let radicalStrokeRanges = new Map();
/**@type {Map<[number, number], number>}*/
let characterNameRanges = new Map();

let maxResults = 100;

for (let [property, propertyIndex] of indexEntries) {
  if (!property.endsWith("RSUnicode") && property !== "kSEAL_Rad") {
    continue;
  }
  for (let [snippetIndex, entry] of propertyIndex) {
    for (let range of entry.characters) {
      radicalStrokeRanges.set(range, {property, snippetIndex});
    }
  }
}

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

function getString(/**@type {number}*/ start) {
  let RECORD_SEPARATOR = "\x1E";
  let limit = allTheStrings.indexOf(RECORD_SEPARATOR, start);
  return allTheStrings.substring(start, limit);
}

function updateQuery(event) {
  if(event.key === 'Enter') {
    let newURL = window.location.protocol + "//" + window.location.host + window.location.pathname
     + "?q=" + encodeURIComponent(document.querySelector('input[name="q"]').value);
    history.replaceState(null, '', newURL);
  }
}

function updateResults(event) {
  /**@type {string}*/
  let query = event.target.value;
  let {entries, rangeCount} = search(query);
  if (rangeCount >= maxResults) {
    document.getElementById("info").innerHTML = `Showing first ${maxResults} results`;
  } else {
    document.getElementById("info").innerHTML = rangeCount + " results";
  }
  document.getElementById("results").innerHTML = entries.join("");
}

function search(/**@type {string}*/ query) {
  let wordBreak = new Intl.Segmenter("en", { granularity: "word" });
  // Override word breaking of ., -, and ' so radical/stroke indices are atomic.
  // This is different from what is done in the index, because we don’t expect a
  // lot of punctuation in the query, but we need to treat partially-type
  // radical-stroke indices as atomic: when indexing, "153." has the word 153,
  // but in a query, it has the word "153." which may be a prefix for "153.9".
  let queryWords = Array.from(
      wordBreak.segment(query.replace(/\.-/g, "pm").replace(/['.]/g, "p")))
      .filter(s => s.isWordLike)
      .map(s => query.substring(s.index, s.index + s.segment.length));
  let foldedQuery = queryWords.map(fold);
  var rangeCount = 0;
  var covered = [];
  /**@type {string[]}*/
  var result = [];
  /**@type {Set<number>}*/
  var resultSnippetIndices = new Set(wordIndex.get(foldedQuery[0])?.keys() ?? []);
  let firstLemmata = [foldedQuery[0]];
  if (resultSnippetIndices.size === 0 && foldedQuery.length == 1) {
    let prefix = fold(queryWords.at(-1));
    for (let [completion, snippets] of wordIndex) {
      if (completion.startsWith(prefix)) {
        firstLemmata.push(completion);
        resultSnippetIndices = resultSnippetIndices.union(snippets);
      }
    }
  }
  for (var i = 1; i < foldedQuery.length; ++i) {
    var rhs = new Set(wordIndex.get(foldedQuery[i])?.keys() ?? []);
    let intersection = resultSnippetIndices.intersection(rhs);
    if (intersection.size === 0 && i == foldedQuery.length - 1) {
      let prefix = fold(queryWords.at(-1));
      for (let [completion, snippets] of wordIndex) {
        if (completion.startsWith(prefix)) {
          rhs = rhs.union(snippets);
        }
      }
      resultSnippetIndices = resultSnippetIndices.intersection(rhs);
    } else {
      resultSnippetIndices = intersection;
    }
  }
  let pivots = firstLemmata.map(l => wordIndex.get(l)).filter(x => !!x);
  let getPivot = (/**@type {number}*/s) => pivots.map(p => p.get(s)).filter(x => x !== undefined)[0];
  let collator = new Intl.Collator("en");
  let sortKeys = new Map(Array.from(resultSnippetIndices).map(
    i => {
      let snippet = getString(i);
      return [i, snippet.substring(getPivot(i)) + ' \uFFFE ' +
                     snippet.substring(0, getPivot(i))];
    }));
  let sortedSnippetIndices = Array.from(resultSnippetIndices).sort(
    (left, right) => collator.compare(
      sortKeys.get(left),
      sortKeys.get(right)));
  for (let propertyIndex of indexEntries.values()) {
    for (let snippetIndex of sortedSnippetIndices) {
      let entry = propertyIndex.get(snippetIndex);
      if (!entry) {
        continue;
      }
      let entrySet = entry.characters;
      if (superset(covered, entrySet)) {
        continue;
      }
      rangeCount += entrySet.length;
      covered = covered.concat(entrySet);
      let pivot = getPivot(snippetIndex);
      let snippet = getString(snippetIndex);
      let tail = snippet.substring(pivot);
      result.push(getString(entry.html).replace(
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
      if (rangeCount >= maxResults) {
        return {entries: result, rangeCount};
      }
    }
  }
  if (queryWords.length <= 1 && query.length > 0) {
    let codePoints = [];
    if (/^[0-9A-F]+$/ui.test(query)) {
      codePoints.push(parseInt(query, 16));
    }
    if (/^.$/ui.test(query)) {
      codePoints.push(query.codePointAt(0));
    }
    for (let cp of codePoints) {
      var name = characterNames.get(cp);
      var rs = null;
      if (!name) {
        for (let [[first, last], {property, snippetIndex}] of radicalStrokeRanges) {
          if (first <= cp && cp <= last) {
            rs = {property, snippetIndex};
            break;
          }
        }
        if (rs) {
          rangeCount += indexEntries.get(rs.property).get(rs.snippetIndex).characters.length;
          result.push(
            getString(indexEntries.get(rs.property).get(rs.snippetIndex).html).replace(
            "[RESULT TEXT]", toHTML(getString(rs.snippetIndex))));
        } else {
          for (let [[first, last], n] of characterNameRanges) {
            if (first <= cp && cp <= last) {
              name = n;
              break;
            }
          }
        }
      }
      if (name) {
        rangeCount += 1;
        result.push(
          getString(indexEntries.get("Name").get(name) ??
                    indexEntries.get("Name_Alias").get(name).html).replace(
          "[RESULT TEXT]", toHTML(getString(name))));
      }
    }
    if (/^boop$/i.test(query)) {
        rangeCount += 1;
      result.push(
        getString(indexEntries.get("Block").get(bettyIndex).html).replace(
        "[RESULT TEXT]", toHTML("Betty")));
    } else if (/^dood$/i.test(query)) {
        rangeCount += 1;
        result.push(
          getString(indexEntries.get("Block").get(theIndex).html).replace(
          "[RESULT TEXT]", toHTML("the")));
    }
  }
  return {entries: result, rangeCount};
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
  var folding = word.normalize("NFKC").toLowerCase();
  return folding.replace("š", "sh");
}

window.onload = function () {
  let params = (new URL(document.location)).searchParams;
  if (params.has("q")) {
    let query = params.get("q");
    document.querySelector('input[name="q"]').value = query;
    updateResults({target: {value: query}});
  }
};
