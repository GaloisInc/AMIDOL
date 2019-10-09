import * as React from "react";
import * as ReactDOM from "react-dom";

import { Hello, Greet, Measures, MeasuresCallbacks } from "./components/Hello";


/** Mount the "Dialog" onto the specified DOM node */
export function showModalDialog(
  mountPoint: HTMLElement,
) {
  const techStack = ['React', 'Typescript', 'Webpack', 'Bootstrap'];
  ReactDOM.render(<Greet techs={techStack} />, mountPoint);
 // ReactDOM.render(<Hello />, mountPoint);
}

declare var reactCallbacks: MeasuresCallbacks;


export function showMeasures(
  mountPoint: HTMLElement,
) {
    ReactDOM.render(<Measures callbacks={reactCallbacks}/>, mountPoint);
}
