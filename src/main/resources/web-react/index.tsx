import * as React from "react";
import * as ReactDOM from "react-dom";

import { Palette, PaletteCallbacks } from "./components/Palette";
import { ButtonBar } from "./components/ButtonBar";
import { Journal } from "./components/ModelNavigation";
import { Measures, MeasuresCallbacks, MeasureProps } from "./components/RewardVariables";
import { PaletteEditor, PaletteItem } from "./components/PaletteEditor";
import { GraphResults, Measure } from "./components/GraphResults";
import { refillSvg, svgColors } from "./utility/Svg.ts";

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

  submitPaletteElement: (string, item) => void,
  deletePaletteElement: (string) => void,

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

export function attachButtonBar(
  mountPoint: HTMLElement,

  journal: Journal,
  onUpload: () => void,
  onDownload: () => void,

  onJuliaUpload: () => void,

  setNewCurrentBackend: (newBackend: string) => void,
  onExecute: () => void,
) {
  ReactDOM.render(
    <ButtonBar
      journal={journal}
      onUpload={onUpload}
      onDownload={onDownload}
      setNewCurrentBackend={setNewCurrentBackend}
      onExecute={onExecute}
      onJuliaUpload={onJuliaUpload}
    />,
    mountPoint,
  );
}

export function attachPalette(
  mountPoint: HTMLElement,

  callbacks: PaletteCallbacks,
  redrawIcons: () => void,
  clearNetwork: (string) => boolean,
) {
  ReactDOM.render(
    <Palette
      redrawIcons={redrawIcons}
      clearNetwork={clearNetwork}
      paletteCallbacks={callbacks}
    />,
    mountPoint
  );
}
