import * as React from "react";
import {
  Spinner,
  Button, Modal, ModalHeader, ModalBody, ModalFooter,
  Dropdown, DropdownItem, DropdownMenu, DropdownToggle,
  Input, InputGroupAddon, InputGroup, InputGroupText, InputGroupButtonDropdown,
  Col, Form, FormGroup, Label, FormText
} from 'reactstrap';
import Plot from 'react-plotly.js';

// `import * from '../utility/Traces';

interface GraphResultsProps {
  closeResults(): void;
  backend: string;
  measures: Measure[];
}

export interface Measure {
  name: string;
  label: string;
  simParams: SimParams;
}

export interface SimParams {
	initialTime: number;
	finalTime: number;
	stepSize: number;
	savePlot?: string;
}

interface GraphResultsState {
  isOpen: boolean;
  datas?: any[];
}

export class GraphResults extends React.Component<GraphResultsProps, GraphResultsState> {

  constructor(props) {
    super(props);

    this.state = {
      isOpen: true
    };

    const endpoint = "/backends/" + this.props.backend + "/integrate";
    Promise
      .all(this.props.measures.map(measure => {
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
      }))
      .then(datas => this.setState({ ...this.state, datas }));

    this.toggle = this.toggle.bind(this);
  }

  toggle() {
    this.setState(prevState => ({ ...prevState, isOpen: !prevState.isOpen }));
  }

  render() {

    // What to put in the modal depends on whether we've got results yet!
    var modalBody = null;
    if (this.state.datas) {
      modalBody = (
        <Plot
          data={this.state.datas}
          style={{width: '100%', height: '100%'}}
          layout={{
            autosize: true,
            showlegend: true,
            margin: {
              l: 50,
              r: 50,
              b: 50,
              t: 50,
              pad: 4
            },
            xaxis: {
              visible: true,
              title: "Time"
            },
            yaxis: {
              visible: true,
            }
          }}
		      useResizeHandler
        />
      )
    } else {
      modalBody = (
        <div style={{display: 'flex', justifyContent: 'center'}}>
          <Spinner color="primary" />
        </div>
      );
    }

    return (
      <Modal
        isOpen={this.state.isOpen}
        onClosed={this.props.closeResults}
        toggle={this.toggle}
        size={'lg'}
        style={{maxWidth: '1600px', width: '80%', margin: '10px auto'}}
        contentClassName={'tall-modal-content'}
        centered
      >
        <ModalHeader toggle={this.toggle}>Model execution results</ModalHeader>
        <ModalBody>{modalBody}</ModalBody>
      </Modal>

    );
  }

}
