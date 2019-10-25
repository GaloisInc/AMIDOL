
export interface Trace {
 x: number[];
 y: number[];
}

/** Returns a new summed data trace, with `x` being the union of `x` values from
 *  the two input traces
 */
function addTraces(trace1: Trace, trace2: Trace): Trace {
  const x1 = trace1.x;
  const x2 = trace2.x;
  const y1 = trace1.y;
  const y2 = trace2.y;

  // Summed trace
  const x3: number[] = [];
  const y3: number[] = [];

  var i1 = 0;  // index through trace1
  var i2 = 0;  // index through trace2
  while (i1 < x1.length || i2 < x2.length) {
    if (x1[i1] == null) {
      // We've run out of entries in `trace1`...
      x3.push(x2[i2]);
      y3.push(y2[i2]);
      i2++;
		} else if (x2[i2] == null) {
      // We've run out of entries in `trace2`...
      x3.push(x1[i1]);
      y3.push(y1[i1]);
      i1++;
		} else if (x1[i1] == x2[i2]) {
      // They share the same `x` value, so just add the `y` values
      x3.push(x1[i1]);
      y3.push(y1[i1] + y2[i2]);
      i2++;
      i1++;
		} else if (x1[i1] < x2[i2] && i2 == 0) {
      // We've not started values in `trace2`
      x3.push(x1[i1]);
      y3.push(y1[i1]);
      i1++;
		} else if (x2[i2] < x1[i1] && i1 == 0) {
      // We've not started values in `trace1`
      x3.push(x2[i2]);
      y3.push(y2[i2]);
      i2++;
		} else if (x1[i1] < x2[i2]) {
      // interpolate the value for `trace2`
      var mult = (x1[i1] - x2[i2-1]) / (x2[i2] - x2[i2-1]);
      var y2_interp = (y2[i2] - y2[i2-1]) * mult + y2[i2-1];
      x3.push(x1[i1]);
      y3.push(y1[i1] + y2_interp);
      i1++;
		} else if (x2[i2] < x1[i1]) {
      // interpolate the value for `trace1`
      var mult = (x2[i2] - x1[i1-1]) / (x1[i1] - x1[i1-1]);
      var y1_interp = (y1[i1] - y1[i1-1]) * mult + y1[i1-1];
      x3.push(x2[i2]);
      y3.push(y2[i2] + y1_interp);
      i2++;
		}
	}

  return {
    x: x3,
    y: y3,
	}
}

