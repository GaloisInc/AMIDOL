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
  equations: string[];
  openPlot: boolean;

  range_start: string;
  range_end: string;
  range_step: string;
}


export class DifferentialEquations extends React.Component<{}, DiffEqState> {
  
  constructor(props) {
    super(props);

    this.state = {
      equations: [],
      openPlot: false,

      range_start: "0",
      range_end: "100",
      range_step: "1",
    };
  }

  render() {
    const rows = this.state.equations.map((eqn: string, idx: number) => {
      const deleteEqn = () => {
        const newEquations = this.state.equations.filter((x, i) => i != idx);
        this.setState({ ...this.state, equations: newEquations });
      };

      const updateEqn = (newEquation: string) => {
        const newEquations = this.state.equations.map((oldEqn,i) => (i == idx) ? newEquation : oldEqn);
        this.setState({ ...this.state, equations: newEquations });
      };

      return <EquationRow
        equation={eqn}
        deleteEquation={deleteEqn}
        updateEquation={updateEqn}
      />;
    });

    const addEquation = () => this.setState({ ...this.state, equations: this.state.equations.concat("") });
    const rangeStartNum = parseFloat(this.state.range_start);
    const rangeEndNum = parseFloat(this.state.range_end);
    const rangeStepNum = parseFloat(this.state.range_step);

    return (
        <Container>
          <h1>Differential equations</h1>
          {rows}
          <Row>
            <Col>
              <Button color="primary" onClick={addEquation}>Add equation</Button>
            </Col>
          </Row>
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
              <Button color="secondary">
              Simulate
              </Button>
            </Col>
          </Row>
        </Container>
    );
  }
}


interface EquationRowProps {
  equation: string;
  updateEquation(newEquation: string);
  deleteEquation();
}

class EquationRow extends React.Component<EquationRowProps, {}> {

  render() {
    const equationSrc = (this.props.equation) ? "$" + this.props.equation + "$" : "";

    return (
      <Row>
        <Col sm={4}>
          <Input
            onChange={(e) => this.props.updateEquation(e.target.value)}
            value={this.props.equation}
          />
        </Col>
        <Col sm={4}>
          <Latex displayMode={true} throwOnError={false}>{equationSrc}</Latex>
        </Col>
        <Col sm={4}>
          <Button color="secondary" onClick={this.props.deleteEquation}>
          Remove equation
          </Button>
        </Col>
      </Row>
    );
  }

}
