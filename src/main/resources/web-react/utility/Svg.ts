/** Given the URL for an SVG, produce a new data URL for the SVG where all
 *	elements that have a fill have been filled with an alternate colour.
 *
 *	NB: the URL _can_ be already itself a data-url
 */
export async function refillSvg(svgSrc: string, newColour: string): Promise<string> {

  // This image is special. Don't allow it to be recoloured!
  if (svgSrc == "images/unknown.png") {
    console.log("Refusing to re-colour the unknown image");
    return svgSrc;
  }

  const svgText: string = await fetch(svgSrc).then(resp => resp.text());
  const parser = new DOMParser();
  const theSvg = parser.parseFromString(svgText, "image/svg+xml").documentElement;

  // Rough query to re-fill anything that already had a fill
  theSvg.querySelectorAll("*").forEach((elem: any) => {
		if (elem.style && elem.style.fill) {
			elem.style.fill = newColour;
		}
  });
  if (theSvg.hasAttribute('fill')) {
	  theSvg.setAttribute('fill',newColour);
  }

	// Turn the SVG into a string, then encode that
	var theString = new XMLSerializer().serializeToString(theSvg);
	return "data:image/svg+xml;base64," + window.btoa(theString);
}


/** Given the URL for an SVG, return its fill colors.
 */
export async function svgColors(svgSrc: string): Promise<string[]> {
  
  const svgText: string = await fetch(svgSrc).then(resp => resp.text());
  const parser = new DOMParser();
  const theSvg = parser.parseFromString(svgText, "image/svg+xml").activeElement;

  const fills: string[] = [];
  theSvg.querySelectorAll("*").forEach((elem: any) => {
		if (elem.style && elem.style.fill) {
			fills.push(elem.style.fill);
		}
  });
  if (theSvg.hasAttribute('fill')) {
	  fills.push(theSvg.getAttribute('fill'));
  }

  return fills;	
}
