import * as React from "react";
import {
  Button, Input, Col, Form, FormGroup, Label, Row,
  ListGroup, ListGroupItem, ListGroupItemHeading, Table
} from 'reactstrap';

export interface TraceSum {
  key: string;
  traces: string[];
}

interface CompareState {
  dataTraces: string[];
  plannedPlot: TraceSum[];
}

interface CompareProps {
  plotComparision(toPlot: TraceSum[]);
}

export class Compare extends React.Component<CompareProps, CompareState> {

  constructor(props) {
    super(props);

    this.state = {
      dataTraces: [],
      plannedPlot: [],
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
        <Button color="primary" onClick={() => this.props.plotComparision(this.state.plannedPlot)}>Plot comparision</Button>
        <br/>
        <br/>
        <UploadSection registerNewDataTrace={(str) => { }} />
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

    return (
      <tr>
        <td>
          <Input sm={6} 
            value={this.props.plannedTrace.key}
            onChange={(e) => this.props.updateTraceSum({ ...this.props.plannedTrace, key: e.target.value })}
          />
        </td>
        <td>
          {
            this.props.plannedTrace.traces.map((trace: string, idx: number) => (<Row>
              <Col><Input
                type="select"
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
        <td>
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
