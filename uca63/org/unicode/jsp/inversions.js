var copyrightInversions = "Copyright © 2000 Mark Davis. All Rights Reserved.";

// Define objects

Inversion.prototype.rangeArray  = [];
Inversion.prototype.opposite  = 0;
Inversion.prototype.getLeast = Inversion_getLeast;
Inversion.prototype.getLeast2 = Inversion_getLeast2;
Inversion.prototype.contains = Inversion_contains;
Inversion.prototype.previous = Inversion_previousDifferent;
Inversion.prototype.next = Inversion_nextDifferent;
Inversion.prototype.makeOpposite = Inversion_makeOpposite;

InversionMap.prototype.inversion = null;
InversionMap.prototype.values = null;
InversionMap.prototype.at = InversionMap_at;

/**
 * Maps integers to a range (half-open).
 * When used as a set, even indices are IN, and odd are OUT.
 * @parameter rangeArray must be an array of monotonically increasing integer values, with at least one instance.
 */
function Inversion (rangeArray) {
  this.rangeArray = rangeArray;
  for (var i = 1; i < rangeArray.length; ++i) {
    if (rangeArray[i] == null) {
      rangeArray[i] = rangeArray[i-1] + 1;
    } else if (!(rangeArray[i-1] < rangeArray[i])) {
      alert("Array must be monotonically increasing! "
       + (i-1) + ": " + rangeArray[i-1]
       + ", " + i + ": " + rangeArray[i]);
      return;
    }
  }
}

/**
 * Creates opposite of this, that is: result.contains(c) iff !this.contains(c)
 * @return reversal of this
 */
function Inversion_makeOpposite () {
  var result = new Inversion(this.rangeArray);
  result.opposite = 1 ^ this.opposite;
  return result;
}

/**
 * @intValue probe value
 * @return true if probe is in the list, false otherwise.
 * Uses the fact than an inversion list
 * contains half-open ranges. An element is
 * in the list iff the smallest index is even
 */
function Inversion_contains(intValue) {
  return ((this.getLeast(intValue) & 1) == this.opposite);
}

/**
 * @intValue probe value
 * @return the largest index such that rangeArray[index] <= intValue.
 * If intValue < rangeArray[0], returns -1.
 */
function Inversion_getLeast (intValue) {
  var arr = this.rangeArray;
  var low = 0;
  var high = arr.length;
  while (high - low > 8) {
    var mid = (high + low) >> 1;
    if (arr[mid] <= intValue) {
      low = mid;
    } else {
      high = mid;
    }
  }
  for (; low < high; ++low) {
    if (intValue < arr[low]) {
      break;
    }
  }
  return low - 1;
}

/*document.mainForm.result.value = "intValue: " + intValue + "\u000D";
if (false) document.mainForm.result.value += 
"arr[" + low + "]=" + arr[low] +
" arr[" + mid + "]=" + arr[mid] +
" arr[" + high + "]=" + arr[high] + "\u000D";
if (false) document.mainForm.result.value += 
"arr[" + low + "]=" + arr[low] +
" arr[" + high + "]=" + arr[high] + "\u000D";
*/

/**
 * @intValue probe value
 * @return the largest index such that rangeArray[index] <= intValue.
 * If intValue < rangeArray[0], returns -1.
 */
function Inversion_getLeast2 (intValue) {
  var arr = this.rangeArray;
  var low = 0;
  var high = arr.length;
  for (; low < high; ++low) {
    if (intValue < arr[low]) {
      break;
    }
  }
  return low - 1;
}

/**
 * @intValue probe value
 * @return next greater probe value that would be different.
 * or null if it would be out of range
 */
function Inversion_nextDifferent(intValue, delta) {
//alert(intValue + ", " + this.rangeArray[this.getLeast(intValue) + 1]);
  return this.rangeArray[this.getLeast(intValue) + delta];
}

/**
 * @intValue probe value
 * @return previous lesser probe value that would be different.
 * or null if it would be out of range
 */
function Inversion_previousDifferent(intValue, delta) {
  return this.rangeArray[this.getLeast(intValue) - delta];
}

/**
 * Maps ranges to values.
 * @parameter rangeArray must be suitable for an Inversion.
 * @parameter valueArray is the list of corresponding values.
 * Length must be the same as rangeArray.
 */
function InversionMap (rangeArray, valueArray) {
  if (rangeArray.length != valueArray.length) {
    return; // error
  }
  this.inversion = new Inversion(rangeArray);
  this.values = valueArray;
}

/**
 * Gets value at range
 * @parameter intValue. Any integer value.
 * @return the value associated with that integer. null if before the first
 * item in the range.
 */
function InversionMap_at (intValue) {
  var index = this.inversion.getLeast(intValue);
  if (index < 0) return null;
  return this.values[index];
}