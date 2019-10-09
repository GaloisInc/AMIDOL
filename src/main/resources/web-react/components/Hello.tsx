import * as React from "react";


import { ListGroup, ListGroupItem } from 'reactstrap';

interface Props {
  techs: string[]
 }

export class Greet extends React.Component<Props, {}> {
  render() {

    const techs = this.props.techs.map((tech) => {
      return <ListGroupItem color="info">{tech}</ListGroupItem>
    })


    return (<div>
      <h1> Welcome to our awesome app</h1>
      <h2>This app has been built using below tech stack </h2>
      <ListGroup>{techs}</ListGroup>
    </div>);
  }
}

// 'HelloProps' describes the shape of props.
// State is never set so we use the '{}' type.
export class Hello extends React.Component<{}, {}> {
  render() {
    return <h1>Hello</h1>;
  }
}


export interface MeasuresCallbacks {
  updateMeasureNouns(MeasuresNoun: string[]): void;
  getMeasures(): MeasureProps[];
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
  until: number,
  step: number,
}

export class Measures extends React.Component<{ callbacks: MeasuresCallbacks }, MeasuresState> {

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
  }

  updateMeasureNouns(newNouns: MeasuresNoun[]) {
    this.setState((prevState) => ({
      measures: (newNouns.length == 0) ? prevState.measures
                                       : prevState.measures.map(m => ({ ...m, nounId: m.nounId || newNouns[0].label })),
      nouns: newNouns
    }));
  }

  getMeasures() {
    return this.state.measures;
  }

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
        until: 100,
        step: 5
      }
    };

    this.setState((prevState) => {
      return {
        measures: prevState.measures.concat(newMeasure)
      };
    })
  }

  deleteMeasure(measureName: string) {
    this.setState((prevState) => {
      return {
        measures: prevState.measures.filter(m => m.name != measureName)
      };
    });
  }

  updateMeasure(measureName: string, newRange: Range, newNounId: string) {
    this.setState((prevState) => {
      return {
        measures: prevState.measures.map((m) => {
          if (m.name == measureName) {
            return { ...m, range: newRange, nounId: newNounId };
          } else {
            return m;
          }
        })
      };
    });
  }

  render() {
    return (
      <div>
      <h2>Measures:</h2>
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
      <button onClick={this.addNewMeasure}>New</button>
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
      <div className="rv_element">
        <h3 className="key">{this.props.name}</h3>
        <select
          value={this.props.nounId}
          onChange={(e) => this.props.updateMeasure(
            this.props.name,
            this.props.range,
            e.target.value
          )}
        >
          {this.props.nouns.map(noun => <option value={noun.label}>{noun.label}</option>)}
        </select>
        <h4>Range</h4>
        <div>
          <select className="opt2">
            <option value="Instant of Time">Instant of Time</option>
            <option value="Interval of Time" disabled>Interval of Time</option>
            <option value="Time Avg. Interval" disabled>Time Avg. Interval</option>
            <option value="Steady State" disabled>Steady State</option>
          </select>
          <div>
            <div className="range_param">
              <label>Start</label>
              <input
                className="range_start"
                value={this.props.range.start}
                size={5}
                onChange={(e) => this.props.updateMeasure(
                  this.props.name,
                  { ...this.props.range, start: parseFloat(e.target.value) },
                  this.props.nounId
                )}
              />
            </div>
            <div className="range_param">
              <label>Until</label>
              <input
                className="range_end"
                value={this.props.range.until}
                size={5}
                onChange={(e) => this.props.updateMeasure(
                  this.props.name,
                  { ...this.props.range, until: parseFloat(e.target.value) },
                  this.props.nounId
                )}
              />
            </div>
            <div className="range_param">
              <label>Step</label>
              <input
                className="range_step"
                value={this.props.range.step}
                size={5}
                onChange={(e) => this.props.updateMeasure(
                  this.props.name,
                  { ...this.props.range, step: parseFloat(e.target.value) },
                  this.props.nounId
                )}
              />
            </div>
          </div>
        </div>
        <button onClick={() => this.props.deleteMeasure(this.props.name)}>Delete</button>
      </div>
    );
  }
}
