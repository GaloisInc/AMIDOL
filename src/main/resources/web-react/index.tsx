import * as React from "react";
import * as ReactDOM from "react-dom";

import { Hello, Greet } from "./components/Hello";
import { Measures, MeasuresCallbacks, MeasureProps } from "./components/RewardVariables";
import { PaletteEditor, PaletteItem } from "./components/PaletteEditor";
import { GraphResults, Measure } from "./components/GraphResults";

declare var reactCallbacks: MeasuresCallbacks;

export function showMeasures(
  mountPoint: HTMLElement,
  setMeasure: (name: string, oldP: MeasureProps, newP: MeasureProps) => void,
) {
  ReactDOM.render(
    <Measures callbacks={reactCallbacks} setMeasure={setMeasure}/>,
    mountPoint
  );
}

/** Mount the "Palette element editor" onto the specified DOM node */
export function showEditNodeDialog(
  mountPoint: HTMLElement,

  submitPaletteElement: (string, item, then) => void,
  deletePaletteElement: (string, then) => void,

  paletteItems: { [itemName: string]: PaletteItem },
  chosenPaletteItemNames: string[],
  editOnOpenItemName: string // undefined if none initially loaded
) {
  const close = () => ReactDOM.unmountComponentAtNode(mountPoint);

  ReactDOM.render(
    <PaletteEditor
      closeEditor={close}
      submitPaletteElement={submitPaletteElement}
      deletePaletteElement={deletePaletteElement}

      paletteItems={paletteItems}
      chosenPaletteItemNames={chosenPaletteItemNames}
      editOnOpenItemName={editOnOpenItemName}
    />,
    mountPoint,
  );
}

interface GraphResultsProps {
  closeResults(): void;
  backend: string;
  measures: Measure[];
}

export function showGraphResults(
  mountPoint: HTMLElement,

  backend: string,
  measures: Measure[],
) {
  const close = () => ReactDOM.unmountComponentAtNode(mountPoint);

  ReactDOM.render(
    <GraphResults
      backend={backend}
      measures={measures}
      closeResults={close}
    />,
    mountPoint,
  );
}
