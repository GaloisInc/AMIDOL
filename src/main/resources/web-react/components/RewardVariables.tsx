import * as React from "react";
import {
  Button, Input, Col, Form, FormGroup, Label, Row,
  ListGroup, ListGroupItem, ListGroupItemHeading
} from 'reactstrap';

export interface MeasuresCallbacks {
  updateMeasureNouns(MeasuresNoun: string[]): void;

  getMeasures(): MeasureProps[];
  setMeasures(m: MeasureProps[]): void;

  addMeasureEffect(newMeasure: MeasureProps);
  deleteMeasureEffect(measureName: string);
  updateMeasureEffect(m: MeasureProps);
}

export interface MeasuresState {
  measures: MeasureProps[],
  nouns: MeasuresNoun[],
}

export interface MeasuresNoun {
  id: string,
  label: string,
}

export interface MeasureProps {
  name: string,
  nounId: string,
  range: Range,
}

export interface Range {
  start: number,
  end: number,
  step: number,
}

export interface MeasuresProps {
  callbacks: MeasuresCallbacks;
  setMeasure: (name: string, oldP: MeasureProps, newP: MeasureProps) => void; 
}

export class Measures extends React.Component<MeasuresProps, MeasuresState> {

  constructor(props) {
    super(props);
    this.state = {
      nouns: [],
      measures: []
    };

    this.addNewMeasure = this.addNewMeasure.bind(this);
    this.deleteMeasure = this.deleteMeasure.bind(this);
    this.updateMeasure = this.updateMeasure.bind(this);

    this.props.callbacks.updateMeasureNouns = this.updateMeasureNouns.bind(this);
    this.props.callbacks.getMeasures = this.getMeasures.bind(this);
    this.props.callbacks.setMeasures = this.setMeasures.bind(this);
    this.props.callbacks.addMeasureEffect = this.addMeasureEffect.bind(this);
    this.props.callbacks.deleteMeasureEffect = this.deleteMeasureEffect.bind(this);
    this.props.callbacks.updateMeasureEffect = this.updateMeasureEffect.bind(this);
  }

  updateMeasureNouns(newNouns: MeasuresNoun[]) {
    this.setState((prevState) => ({
      measures: (newNouns.length == 0) ? prevState.measures
                                       : prevState.measures.map(m => ({ ...m, nounId: m.nounId || newNouns[0].label })),
      nouns: newNouns
    }));
  }

  /*
   * These functions inform the global journal of a state change (so don't call
   * setState!
   */

  addNewMeasure() {
    const name = prompt("Name your new reward variable:");

    // Bail out early if the user cancelled
    if (!name)
      return;

    // Bail out early if the name entered is already a reward variable
    if (this.state.measures.some(m => m.name == name))
      return;

    // TODO: guess this based on name?
    const noun = this.state.nouns[0];

    const newMeasure: MeasureProps = {
      name,
      nounId: noun && noun.id,
      range: {
        start: 0,
        end: 100,
        step: 5
      }
    };

    this.props.setMeasure(newMeasure.name, null, newMeasure);
  }

  deleteMeasure(measureName: string) {
    this.props.setMeasure(measureName, this.state.measures[measureName], null);
  }

  updateMeasure(measureName: string, newRange: Range, newNounId: string) {
    const m = this.state.measures.filter((measure) => measure.name == measureName)[0];
    const newMeasure = { ...m, range: newRange, nounId: newNounId };

    this.props.setMeasure(newMeasure.name, m, newMeasure);
  }

  /*
   * These functions are the callbacks used to update after a state change (so
   * call `setState`)
   */

  addMeasureEffect(newMeasure: MeasureProps) {
    this.setState((prevState) => {
      return {
        measures: prevState.measures.concat(newMeasure)
      };
    });
  }

  deleteMeasureEffect(measureName: string) {
    this.setState((prevState) => {
      return {
        measures: prevState.measures.filter(m => m.name != measureName)
      };
    });
  }

  updateMeasureEffect(newMeasure: MeasureProps) {
    this.setState((prevState) => ({
      ...prevState,
      measures: prevState.measures.map((m) => {
        if (m.name == newMeasure.name) {
          return newMeasure;
        } else {
          return m;
        }
      })
    }));
  }


  // Only for debugging
  getMeasures() {
    return this.state.measures;
  }
  setMeasures(newMeasures: MeasureProps[]) {
    this.setState(prevState => ({
      ...prevState,
      measures: newMeasures,
    }));
  }

  render() {
    return (
      <div>
      <ListGroup flush>
      {
        this.state.measures.map((m) => (
          <Measure
            name={m.name}
            nounId={m.nounId}
            range={m.range}
            deleteMeasure={this.deleteMeasure}
            updateMeasure={this.updateMeasure}
            nouns={this.state.nouns}
          />
        ))
      }
      <ListGroupItem>
      <Button color="primary" onClick={this.addNewMeasure} block>Add new</Button>
      </ListGroupItem>
      </ListGroup>
      </div>
    );
  }
}

export interface MeasureCallbacks {
  deleteMeasure(measureName: string): void;
  updateMeasure(measureName: string, newRange: Range, newNounId: string): void;
}

export class Measure extends React.Component<MeasureProps & MeasureCallbacks & { nouns: MeasuresNoun[] }, {}> {
  render() {
    return (
      <ListGroupItem>
        <ListGroupItemHeading>{this.props.name}</ListGroupItemHeading>
        <FormGroup row>
          <Label for="stateVariableTracked" sm={4}>Tracking</Label>
          <Col sm={8}>
            <Input
              type="select"
              id="stateVariableTracked"
              value={this.props.nounId}
              onChange={(e) => this.props.updateMeasure(
                this.props.name,
                this.props.range,
                e.target.value
              )}
            >
              {
                this.props.nouns.map(noun =>
                  <option value={noun.id}>{noun.label}</option>
                )
              }
            </Input>
          </Col>
        </FormGroup>
        <FormGroup row>
          <Label for="measureSort" sm={4}>Type</Label>
          <Col sm={8}>
            <Input type="select" id="measureSort">
              <option value="Instant of Time">Instant of Time</option>
              <option value="Interval of Time" disabled>Interval of Time</option>
              <option value="Time Avg. Interval" disabled>Time Avg. Interval</option>
              <option value="Steady State" disabled>Steady State</option>
            </Input>
          </Col>
        </FormGroup>
        <Row>
          <Col sm={4}>
            <FormGroup>
              <Label for="range_start">Start</Label>
              <Input
                type="text"
                id="range_start"
                value={this.props.range.start}
                onChange={(e) => this.props.updateMeasure(
                  this.props.name,
                  { ...this.props.range, start: parseFloat(e.target.value) },
                  this.props.nounId
                )}
              />
            </FormGroup>
          </Col>
          <Col sm={4}>
            <FormGroup>
              <Label for="range_end">Until</Label>
              <Input
                type="text"
                id="range_end"
                value={this.props.range.end}
                size={5}
                onChange={(e) => this.props.updateMeasure(
                  this.props.name,
                  { ...this.props.range, end: parseFloat(e.target.value) },
                  this.props.nounId
                )}
              />
            </FormGroup>
          </Col>
          <Col sm={4}>
            <FormGroup>
              <Label for="range_step">Step</Label>
              <Input
                type="text"
                id="range_step"
                value={this.props.range.step}
                onChange={(e) => this.props.updateMeasure(
                  this.props.name,
                  { ...this.props.range, step: parseFloat(e.target.value) },
                  this.props.nounId
                )}
              />
            </FormGroup>
          </Col>
        </Row>
        <Button
          color="secondary"
          onClick={() => this.props.deleteMeasure(this.props.name)}
          block
        >Remove</Button>
      </ListGroupItem>
    );
  }
}
