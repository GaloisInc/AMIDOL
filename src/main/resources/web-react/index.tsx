import * as React from "react";
import * as ReactDOM from "react-dom";

import { Palette, PaletteCallbacks } from "./components/Palette";
import { ButtonBar } from "./components/ButtonBar";
import { Journal } from "./components/ModelNavigation";
import { Measures, MeasuresCallbacks, MeasureProps } from "./components/RewardVariables";
import { PaletteEditor, PaletteItem } from "./components/PaletteEditor";
import { GraphResults, Measure } from "./components/GraphResults";
import { refillSvg, svgColors } from "./utility/Svg.ts";
import { TraceSum, Compare } from "./components/Compare";

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


export function showGraphResults(
  mountPoint: HTMLElement,

  backend: string,
  measures: Measure[],
) {
  const close = () => ReactDOM.unmountComponentAtNode(mountPoint);

  const endpoint = "/backends/" + backend + "/integrate";
  const datas = Promise
      .all(measures.map(measure => {
        const restOptions = {
          method: "POST",
          headers: {
            'Accept': 'application/json',
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(measure.simParams)
        };
        return fetch(endpoint, restOptions)
          .then(result => result.json())
          .then(dataResult => {
            return {
              name: measure.name,
              x: dataResult.time,
              y: dataResult.variables[measure.label],
              type: 'scatter',
              mode: 'lines+points',
          //    marker: { color: 'red' }
            };
          });
      }));


  ReactDOM.render(
    <GraphResults
      datasPromise={datas}
      closeResults={close}
      title={"Model execution results"}
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

export function attachCompare(
  mountPoint: HTMLElement,

  plot: (toPlot: TraceSum[]) => void,
) {
  ReactDOM.render(
    <Compare plotComparision={plot} />,
    mountPoint
  );
}
