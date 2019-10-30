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
  title: string;
  datasPromise: Promise<any[]>;
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
    this.toggle = this.toggle.bind(this);

    this.props.datasPromise.then(datas => this.setState({ ...this.state, datas }));
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
        <ModalHeader toggle={this.toggle}>{this.props.title}</ModalHeader>
        <ModalBody>{modalBody}</ModalBody>
      </Modal>

    );
  }

}
