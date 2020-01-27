import * as React from "react";
import {
  Button, Input, Col, Form, FormGroup, Label, Row,
  ListGroup, ListGroupItem, ListGroupItemHeading, Table,
  TabPane, TabContent, Popover, PopoverHeader, PopoverBody,
  InputGroup, InputGroupAddon, Container
} from 'reactstrap';
import { GraphResults } from './GraphResults';
import * as Latex from "react-latex";


interface DiffEqState {
  equations: string;
  openPlot: boolean;

  range_start: string;
  range_end: string;
  range_step: string;
}


export class DifferentialEquations extends React.Component<{}, DiffEqState> {
  promisedData: Promise<any[]>;

  constructor(props) {
    super(props);

    this.state = {
      equations: "",
      openPlot: false,

      range_start: "0",
      range_end: "100",
      range_step: "1",
    };

    this.submit = this.submit.bind(this);
  }

  submit(differentialEquations: string[]) {
    const uiDiffEqsData = new URLSearchParams();
    uiDiffEqsData.append("equations", JSON.stringify({ equations: differentialEquations }));

    const simParams = {
      initialTime: parseFloat(this.state.range_start),
      finalTime: parseFloat(this.state.range_end),
      stepSize: parseFloat(this.state.range_step),
      savePlot: null
    };
    const integrateData = JSON.stringify(simParams);

    // Put the model. Once that is done, run the simulation
    this.setState({ ...this.state, openPlot: true });
    this.promisedData = fetch("/appstate/uiDiffEqs", { method: 'POST', body: uiDiffEqsData })
      .then(resp => {
        if (resp.ok) {
          return resp;
        } else {
          return resp.text().then(txt => { throw txt; });
        }
      })
      .then(() => fetch("/backends/scipy/integrate", {
        method: 'POST',
        headers: {
          'Accept': 'application/json',
          'Content-Type': 'application/json'
        },
        body: integrateData
      }))
      .then(resp => {
        if (resp.ok) {
          return resp.json();
        } else {
          return resp.text().then(txt => { throw txt; });
        }
      })
      .then(dataResult => {
        return Object.keys(dataResult.variables).map(key => {
          return {
            name: key,
            x: dataResult.time,
            y: dataResult.variables[key],
            type: 'scatter',
            mode: 'lines+points'
          };
        });
      });
  }

  render() {
    const equations = this.state.equations
      .split("\n")
      .map(eqn => eqn.trim())
      .filter(eqn => eqn.length > 0);

    const updateEqns = (newEquations: string) => {
      this.setState({ ...this.state, equations: newEquations });
    };

    const rangeStartNum = parseFloat(this.state.range_start);
    const rangeEndNum = parseFloat(this.state.range_end);
    const rangeStepNum = parseFloat(this.state.range_step);

    let graphResults = null;
    if (this.state.openPlot && this.promisedData !== undefined) {
      graphResults = <GraphResults
        datasPromise={this.promisedData}
        closeResults={() => {
          this.setState({ ...this.state, openPlot: false });
          this.promisedData = undefined;
        }}
        title={"Differential Equations Simulation"}
      />;
    }

    return (
      <div>
            <Container>
          <h1>Differential equations</h1>
          <div>
     Enter equations matching these structures:

            <ul>
              <li>
                A differential equation of the form <Latex displayMode={false}>{"$\\frac{dX}{dt} = \\cdots$"}</Latex>
              </li>
              <li>
                An initial condition of the form <Latex displayMode={false}>{"$X_0 = \\cdots$"}</Latex>
              </li>
              <li>
                A constant of the form <Latex displayMode={false}>{"$k = \\cdots$"}</Latex>
              </li>
            </ul>
          </div>
          <EquationArea
            equations={this.state.equations}
            updateEquations={updateEqns}
          />
          <br/>
          <br/>
          <h3>Simulation parameters</h3>
          <Row>
            <Col sm={3}>
              <FormGroup>
                <Label for="range_start">Start</Label>
                <Input
                  id="range_start"
                  value={this.state.range_start}
                  onChange={(e) => this.setState({ ...this.state, range_start: e.target.value })}
                  invalid={!rangeEndNum}
                />
              </FormGroup>
            </Col>
            <Col sm={3}>
              <FormGroup>
                <Label for="range_end">Until</Label>
                <Input
                  id="range_end"
                  value={this.state.range_end}
                  onChange={(e) => this.setState({ ...this.state, range_end: e.target.value })}
                  invalid={!rangeStepNum}
                />
              </FormGroup>
            </Col>
            <Col sm={3}>
              <FormGroup>
                <Label for="range_step">Step</Label>
                <Input
                  id="range_step"
                  value={this.state.range_step}
                  onChange={(e) => this.setState({ ...this.state, range_step: e.target.value })}
                  invalid={!rangeStepNum}
                />
              </FormGroup>
            </Col>
            <Col sm={3}>
              <Button color="secondary" onClick={() => this.submit(equations)}>
              Simulate
              </Button>
            </Col>
          </Row>
        </Container>
        {graphResults}
      </div>
    );
  }
}


interface EquationRowProps {
  equations: string;
  updateEquations(newEquations: string);
}

class EquationArea extends React.Component<EquationRowProps, {}> {

  render() {
    const equationsSrc = this.props.equations
      .split("\n")
      .map(eqn => eqn.trim())
      .filter(eqn => eqn.length > 0)
      .map(eqn => "$" + eqn + "$");

    return (
      <Row>
        <Col sm={6}>
          <InputGroup>
            <Input
              type="textarea"
              rows={10}
              style={{fontFamily: 'monospace', height: '100%'}}
              onChange={(e) => this.props.updateEquations(e.target.value)}
              value={this.props.equations}
            />
          </InputGroup>
        </Col>
        <Col sm={6}>
          {
            equationsSrc.map((eqnSrc: string) =>
              <Row>
                <Latex displayMode={true} throwOnError={false}>{eqnSrc}</Latex>
              </Row>
            )
          }
        </Col>
      </Row>
    );
  }

}
