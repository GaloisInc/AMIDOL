/** Given the URL for an SVG, produce a new data URL for the SVG where all
 *  elements that have a fill have been filled with an alternate colour.
 *
 *  NB: the URL _can_ be already itself a data-url
 */
function refillSvg(svgSrc, newColour, onComplete) {
  $.get(svgSrc, function(response) {
    // We expect a `SVGSVGElement`
		var theSvg = response.rootElement;

    // Rough jQuery to re-fill anything that already had a fill
    $(theSvg).find('*').each(function (index, elem) {
				if (elem.style && elem.style.fill) {
          elem.style.fill = newColour;
				}
		});

    // Turn the SVG into a string, then encode that
    var theString = new XMLSerializer().serializeToString(theSvg);
    onComplete("data:image/svg+xml;base64," + window.btoa(theString));
	})
}

