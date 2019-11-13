import * as React from "react";
import {
  Button, Input, Col, Form, FormGroup, Label, Row,
  ListGroup, ListGroupItem, ListGroupItemHeading, Table,
  TabPane, TabContent, Popover, PopoverHeader, PopoverBody,
  InputGroup, InputGroupAddon,
} from 'reactstrap';
import { addTraces } from '../utility/Traces.ts';
import { GraphResults } from './GraphResults';

export interface TraceSum {
  key: string;

  // One of `traces` and `equation` is expected to be non-null
  traces?: string[];
  equation?: string;
}

interface CompareState {
  dataTraces: string[];
  plannedPlot: TraceSum[];
  openPlot: boolean;
}

export class Compare extends React.Component<{}, CompareState> {
  promisedData: Promise<any[]>;

  constructor(props) {
    super(props);

    this.state = {
      dataTraces: [],
      plannedPlot: [],
      openPlot: false,
    };

    setInterval(
      () => {
        const data = new URLSearchParams();
        data.append("limit", JSON.stringify(100));
        fetch("/appstate/data-traces/list", { method: 'POST', body: data })
          .then(resp => resp.json())
          .then(resp => this.setState({ ...this.state, dataTraces: resp }));
      },
      5000,
    );
  }

  produceChartData(): Promise<any[]> {
    return Promise.all(this.state.plannedPlot.map(dataTrace => {
      const data = new URLSearchParams();
      const equation = (dataTrace.equation)
        ? dataTrace.equation
        : dataTrace.traces.map(x => '`' + x + '`').join("+")
      data.append("query", equation);
      return fetch("/appstate/data-traces/eval", { method: 'POST', body: data })
        .then(resp => resp.json())
        .then(resp => {
          console.log(resp);
          return {
            x: resp[0],
            y: resp[1],
            mode: 'lines+markers',
            name: dataTrace.key
          };
        });
    }));
  }

  render() {

    const addDataTrace = () => {
      const key: string = prompt("Name your new trace:");
      if (key) {
        this.setState({ ...this.state, plannedPlot: this.state.plannedPlot.concat({ key, traces: [] }) });
      }
    };

    const tableRows = this.state.plannedPlot.map((sum: TraceSum, idx: number) => {

      const deleteTraceSum = () => {
        const newPlanned = this.state.plannedPlot.filter((x, i) => i != idx);
        this.setState({ ...this.state, plannedPlot: newPlanned });
      };

      const updateTraceSum = (newSum: TraceSum) => {
        const newPlanned = this.state.plannedPlot.map((oldSum,i) => (i == idx) ? newSum : oldSum);
        this.setState({ ...this.state, plannedPlot: newPlanned });
      };

      return (
        <DataRow
          plannedTrace={sum}
          getDataTraces={() => this.state.dataTraces}
          updateTraceSum={updateTraceSum}
          deleteTraceSum={deleteTraceSum}
        />
      );
    });

    let graphResults = null;
    if (this.state.openPlot && this.promisedData !== undefined) {
      graphResults = <GraphResults
        datasPromise={this.promisedData}
        closeResults={() => {
          this.setState({ ...this.state, openPlot: false });
          this.promisedData = undefined;
        }}
        title={"Comparision plot"}
      />;
    }

    const triggerPlot = () => {
      this.promisedData = this.produceChartData();
      this.setState({ ...this.state, openPlot: true });
    };

    return (
      <div>
        <h2>Models to compare</h2>
        <Table>
          <thead>
            <tr>
              <th>Series name</th>
              <th>Data traces</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {tableRows}
          </tbody>
        </Table>
        <Button color="primary" onClick={addDataTrace}>Add new</Button>&nbsp;&nbsp;
        <Button color="primary" onClick={triggerPlot}>Plot comparision</Button>
        <br/>
        <br/>
        <UploadSection registerNewDataTrace={(str) => { }} />
        {graphResults}
      </div>
    );
  }

}

interface DataRowProps {
  plannedTrace: TraceSum;
  getDataTraces(): string[];
  updateTraceSum(newPlannedTrace: TraceSum);
  deleteTraceSum();
}

export class DataRow extends React.Component<DataRowProps, {}> {

  parseEquation(eqn: string): string[] | null {
    let remainingEqn = eqn.trim();

    // The empty case is special
    if (remainingEqn === "") {
      return [];
    }

    const variableRegex = /^\+\s*`([^`]+)`\s*(.*)$/;
    const wip = [];
    remainingEqn = "+" + remainingEqn;

    // As long as there is input, parse a `+ <variable>` off the front.
    while (remainingEqn != "") {
      const match = remainingEqn.match(variableRegex);

      // Failed parse
      if (!match) return null;

      wip.push(match[1]);
      remainingEqn = match[2];
    }

    return wip;
  }

  render() {
    const addAddend = () => {
      const dataTraces = this.props.getDataTraces();
      if (dataTraces[0] === undefined) {
        alert(
          "You cannot add a data trace to this series, since " +
       	  "there are no data traces to chose from!"
        );
      } else {
        const newTraces = this.props.plannedTrace.traces.concat(dataTraces[0]);
        this.props.updateTraceSum({ ...this.props.plannedTrace, traces: newTraces });
      }
    };

    const removeAddend = (idx: number) => {
      const newTraces = this.props.plannedTrace.traces.filter((x,i) => i != idx);
      this.props.updateTraceSum({ ...this.props.plannedTrace, traces: newTraces });
    };

    const updateAddend = (idx: number, newTrace: string) => {
      const newTraces = this.props.plannedTrace.traces.map((oldTrace,i) => (i == idx) ? newTrace : oldTrace);
      this.props.updateTraceSum({ ...this.props.plannedTrace, traces: newTraces });
    };

    const updateEquation = (newEquation: string) => {
      this.props.updateTraceSum({ ...this.props.plannedTrace, equation: newEquation });
    };

    let disableSwapButton = false;
    let swapButtonText = "";
    let onSwapClick = () => { };

    if (this.props.plannedTrace.traces) {
      swapButtonText = "Advanced";

      onSwapClick = () => this.props.updateTraceSum({
        key: this.props.plannedTrace.key,
        traces: null,
        equation: this.props.plannedTrace.traces.map(x => "`" + x + "`").join(" + "),
      });
    } else {
      swapButtonText = "Simple";

      const traces = this.parseEquation(this.props.plannedTrace.equation);
      if (traces !== null) {
        onSwapClick = () => this.props.updateTraceSum({
          key: this.props.plannedTrace.key,
          traces,
          equation: null,
        });
      } else {
        disableSwapButton = true;
      }
    }

   const editorCell = (swapButtonText === "Advanced")
     ? (
       <td>
       {
         this.props.plannedTrace.traces.map((trace: string, idx: number) => (<Row>
           <Col><Input
             type="select"
             value={trace}
             onChange={(e) => updateAddend(idx, e.target.value)}
           >
             {
               this.props.getDataTraces().map(dataTraceName => <option>{dataTraceName}</option>)
             }
           </Input></Col>

           <Col><Button
             color="secondary"
             onClick={() => removeAddend(idx)}
           >Remove trace</Button>
           &nbsp;&nbsp;{'+'}</Col>
           </Row>
         ))
       }
       <Row><Col><Button
         color="primary"
         onClick={addAddend}
       >...</Button></Col></Row>
       </td>
     )
     : (
       <td>
         <InputGroup>
           <Input
             onChange={(e) => updateEquation(e.target.value)}
             value={this.props.plannedTrace.equation}
           />
           <InputGroupAddon addonType="append">
             <HelpPopover/>
           </InputGroupAddon>
         </InputGroup>
       </td>
     );


    return (
      <tr>
        <td>
          <Input sm={6}
            value={this.props.plannedTrace.key}
            onChange={(e) => this.props.updateTraceSum({ ...this.props.plannedTrace, key: e.target.value })}
          />
        </td>
        {editorCell}
        <td>
          <Button
            color="secondary"
            onClick={onSwapClick}
            disabled={disableSwapButton}
          >{swapButtonText}</Button>
          &nbsp;&nbsp;
          <Button
            color="secondary"
            onClick={this.props.deleteTraceSum}
          >Remove series</Button>
        </td>
      </tr>
    );
  }

}


interface UploadProps {
  registerNewDataTrace(traceName: string);
}

interface UploadState {
  nameValue: string;
  fileValue: string;
}

export class UploadSection extends React.Component<UploadProps, UploadState> {
  files?: FileList;

  constructor(props) {
    super(props);
    this.state = { nameValue: "", fileValue: "" };
  }
  
  render() {

    const newDataTrace = () => {
      if (this.state.nameValue && this.state.fileValue) {
        const reader = new FileReader()
				reader.onload = () => {
					const data = JSON.parse(reader.result as string);
          const payload = new URLSearchParams();
				  payload.append("name", this.state.nameValue);
					payload.append("data", JSON.stringify(data.data));
					payload.append("time", JSON.stringify(data.time));
          fetch("/appstate/data-traces/put", { method: 'POST', body: payload })
            .then(() => {
              this.props.registerNewDataTrace(this.state.nameValue);
						  alert("Data source " + name + " has been uploaded");
              this.setState({ nameValue: "", fileValue: "" });
            });
				};
        reader.readAsText(this.files[0]);
      } else {
        alert("You need to enter a file and data trace name!");
      }
    };

    return (
      <div>
        <h2>Upload a new data trace</h2>
        <Row>
          <Label for="data-trace-name" sm={4}>Name</Label>
          <Col sm={8}>
            <Input
              id="data-trace-name"
              type="text"
              value={this.state.nameValue}
              onChange={(e) => this.setState({ ...this.state, nameValue: e.target.value })}
            />
          </Col>
        </Row>
        <Row>
          <Label for="data-trace-file" sm={4}>Data</Label>
          <Col sm={8}>
            <Input
              id="data-trace-file"
              type="file"
              value={this.state.fileValue}
              onChange={(e) => {
                this.files = e.target.files;
                this.setState({ ...this.state, fileValue: e.target.value });
              }}
            />
          </Col>
        </Row>
        <Button 
          color="primary"
          onClick={newDataTrace}
        >Upload series</Button>
      </div>
    );
  }

}


export class HelpPopover extends React.Component<{}, { isOpen: boolean }> {

  constructor(props) {
    super(props);
    this.state = { isOpen: false };
  }

  render() {

    const toggle = () => this.setState({ isOpen: !this.state.isOpen });

    return (
      <div>
        <Button id="OpenHelp">Help</Button>
        <Popover placement="bottom" isOpen={this.state.isOpen} target="OpenHelp" toggle={toggle}>
          <PopoverHeader>Advanced equation editor</PopoverHeader>
          <PopoverBody>
            This equation editor lets you combine and manipulate data
            traces in more interesting ways, although you must manually write
            these equations out. For example:<br/><br/>

            <code>
            `trace 1` + shift(2 * `trace 2`, 35.0)
            </code><br/><br/>

            <ul>
              <li>
                Traces are referred to by their names, although names including
                special characters (such as spaces or dashes) need to be
                enclosed in backquotes. For example, <code>x</code> or
                <code>`my trace`</code>
              </li>

              <li>
                Ordinary arithmetic operations <code>+</code>, <code>-</code>,
                <code>*</code>, and <code>/</code> combine two traces by
                applying the operation at every time point
              </li>

              <li>
                Number literals such as <code>2</code>, <code>1.3e2</code>, or
                <code>-3.14159</code> represent traces with that constant
                numeric value everywhere on their domain
              </li>

              <li>
                There are a handful of special functions that can be used to
                create special traces
                <ul>
                  <li><code>time()</code> produces an identity trace (eg. <code>f(t) = t</code>)</li>
                  <li><code>shift(trace, 2.0)</code> shifts <code>trace</code> to the right by <code>2.0</code></li>
                  <li><code>sin(trace)</code> applies the <code>sin</code> function to each value of <code>trace</code></li>
                </ul>
              </li>
            </ul>
          </PopoverBody>
        </Popover>
      </div>
    );
  }

}
