// Lemma to snippet (compressed) to position of the word in the snippet.
/**@type {Map<string, Map<number, number>>}*/
let wordIndex/*= GENERATED LINE*/;
// Property name to snippet (compressed) to index entry; the html is compressed.
/**@type {Map<string, Map<number, {html: number, characters: ([number]|[number,number])[]}>>}*/
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
let allTheStringsReady = new Response(
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
      let [first, last] = [range[0], range.at(-1)];
      radicalStrokeRanges.set([first, last], {property, snippetIndex});
    }
  }
}

for (let [name, entry] of indexEntries.get("Name")) {
  if (entry.characters[0][0] == entry.characters[0].at(-1)) {
    characterNames.set(entry.characters[0][0], name);
  } else {
    for (let range of entry.characters) {
      let [first, last] = [range[0], range.at(-1)];
      characterNameRanges.set([first, last], name);
    }
  }
}
for (let [name, entry] of indexEntries.get("Name_Alias")) {
  if (!characterNames.has(entry.characters[0][0])) {
    characterNames.set(entry.characters[0][0], name);
  }
}

async function getString(/**@type {number}*/ start) {
  let RECORD_SEPARATOR = "\x1E";
  await allTheStringsReady;
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

async function updateResults(event) {
  /**@type {string}*/
  let query = event.target.value;
  let {entries, rangeCount} = await search(query);
  if (rangeCount >= maxResults) {
    document.getElementById("info").innerHTML = `Showing first ${maxResults} results`;
  } else {
    document.getElementById("info").innerHTML = rangeCount + " results";
  }
  document.getElementById("results").innerHTML = entries.join("");
}

async function search(/**@type {string}*/ query) {
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
  /**@type {Set<number>}*/
  let fullLemmaMatches;
  let firstLemmata = [foldedQuery[0]];
  let matchedLemmata = [...foldedQuery];
  if (foldedQuery.length == 1) {
    fullLemmaMatches = new Set(resultSnippetIndices);
    if (fullLemmaMatches.size < maxResults) {
      let prefix = fold(queryWords.at(-1));
      for (let [completion, snippets] of wordIndex) {
        if (completion.startsWith(prefix)) {
          matchedLemmata.push(completion);
          firstLemmata.push(completion);
          resultSnippetIndices = resultSnippetIndices.union(snippets);
        }
      }
    }
  }
  for (var i = 1; i < foldedQuery.length; ++i) {
    var rhs = new Set(wordIndex.get(foldedQuery[i])?.keys() ?? []);
    let intersection = resultSnippetIndices.intersection(rhs);
    if (i == foldedQuery.length - 1) {
      fullLemmaMatches = new Set(intersection);
      if (fullLemmaMatches.size < maxResults) {
        let prefix = fold(queryWords.at(-1));
        for (let [completion, snippets] of wordIndex) {
          if (completion.startsWith(prefix)) {
            matchedLemmata.push(completion);
            rhs = rhs.union(snippets);
          }
        }
      }
      resultSnippetIndices = resultSnippetIndices.intersection(rhs);
    } else {
      resultSnippetIndices = intersection;
    }
  }
  let pivots = firstLemmata.map(l => wordIndex.get(l)).filter(x => !!x);
  let getPivot = (/**@type {number}*/s) => pivots.map(p => p.get(s)).filter(x => x !== undefined)[0];
  let allMatchStarts = matchedLemmata.map(l => wordIndex.get(l)).filter(x => !!x);
  let getMatchStarts = (/**@type {number}*/s) =>
      allMatchStarts.map(p => p.get(s)).filter(x => x !== undefined);
  let collator = new Intl.Collator("en");
  let sortKeys = new Map(await Promise.all(Array.from(resultSnippetIndices).map(
    async i => {
      let snippet = await getString(i);
      let pivot = getPivot(i);
      let matchStarts = getMatchStarts(i);
      let previousMatch = -1;
      let orderedMatches = 1;
      for (let start of matchStarts) {
        if (start > previousMatch) {
          ++orderedMatches;
          previousMatch = start;
        } else {
          break;
        }
      }
      return [i,
              String.fromCodePoint(0x10FFFE - orderedMatches) + '\uFFFE' +
              snippet.substring(pivot) + ' \uFFFE ' +
              snippet.substring(0, pivot)];
    })));
  let sortedSnippetIndices = Array.from(resultSnippetIndices).sort(
    (left, right) => collator.compare(
      sortKeys.get(left),
      sortKeys.get(right)));
  for (let snippetSet of [fullLemmaMatches, resultSnippetIndices]) {
    for (let propertyIndex of indexEntries.values()) {
      for (let snippetIndex of sortedSnippetIndices) {
        if (!snippetSet.has(snippetIndex)) {
          continue;
        }
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
        let matchStarts = new Set(getMatchStarts(snippetIndex));
        let snippet = await getString(snippetIndex);
        /**@type {string[]|string}*/
        let head = [];
        /**@type {string[]|string}*/
        let tail = [];
        let tagma = head;
        for (let segment of wordBreak.segment(snippet)) {
          if (segment.index >= pivot) {
            tagma = tail;
          }
          if (matchStarts.has(segment.index)) {
            tagma.push(`<b>${toHTML(segment.segment)}</b>`);
          } else {
            tagma.push(toHTML(segment.segment));
          }
        }
        head = head.join("");
        tail = tail.join("");
        result.push((await getString(entry.html)).replace(
          "[RESULT TEXT]",
          "<span class=tail" +
          (snippet.includes(",") ? " style=width:100%" : "") + ">" +
          tail +
          (pivot > 0 && !tail.endsWith(".") ? "," : "") +
          "</span> " +
            (pivot > 0 ? "<span class=head>" +
                        head +
                        "</span>"
                      : "")));
        if (rangeCount >= maxResults) {
          return {entries: result, rangeCount};
        }
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
            (await getString(indexEntries.get(rs.property).get(rs.snippetIndex).html)).replace(
            "[RESULT TEXT]", toHTML(await getString(rs.snippetIndex))));
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
          (await getString(
              (indexEntries.get("Name").get(name) ??
               indexEntries.get("Name_Alias").get(name)).html)).replace(
          "[RESULT TEXT]", toHTML(await getString(name))));
      }
    }
    if (/^boop$/i.test(query)) {
        rangeCount += 1;
      result.push(
        (await getString(indexEntries.get("Block").get(bettyIndex).html)).replace(
        "[RESULT TEXT]", toHTML("Betty")));
    } else if (/^dood$/i.test(query)) {
        rangeCount += 1;
        result.push(
          (await getString(indexEntries.get("Block").get(theIndex).html)).replace(
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

function superset(/**@type {([number, number]|[number])[]}*/left,
                  /**@type {([number, number]|[number])[]}*/right) {
  var remaining = right.slice();
  for (containingRange of left) {
    remaining = remaining.flatMap(r => rangeMinus(r, containingRange));
  }
  if (remaining.length > 0) {
    return false;
  }
  return true;
}

function rangeMinus(/**@type {[number, number]|[number]}*/left,
                    /**@type {[number, number]|[number]}*/right) {
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
    if (left.at(-1) > intersection.at(-1)) {
      result.push([intersection.at(-1) + 1, left.at(-1) - 1]);
    }
    return result;
  }
}

function rangeIntersection(/**@type {[number, number]|[number]}*/left,
                           /**@type {[number, number]|[number]}*/right) {
  let [leftStart, leftEnd] = [left[0], left.at(-1)];
  let [rightStart, rightEnd] = [right[0], right.at(-1)];
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
