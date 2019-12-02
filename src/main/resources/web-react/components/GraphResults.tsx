import * as React from "react";
import { Spinner, Button, Modal, ModalHeader, ModalBody, ModalFooter } from 'reactstrap';
import Plot from 'react-plotly.js';

interface GraphResultsProps {
  // What to do when the graph is closed
  closeResults(): void;

  // Title for the plot
  title: string;

  // Data with which to populate the graph
  datasPromise: Promise<any[]>;
}

interface GraphResultsState {
  isOpen: boolean;
  datas?: any[];
  error?: any;
}

export class GraphResults extends React.Component<GraphResultsProps, GraphResultsState> {

  constructor(props) {
    super(props);

    this.state = {
      isOpen: true
    };
    this.toggle = this.toggle.bind(this);

    this.props.datasPromise
      .then(datas => this.setState({ ...this.state, datas }))
      .catch(error => this.setState({ ...this.state, error }));
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
      );
    } else if (this.state.error) {
      modalBody = (
        <pre style={{color: "red"}}>{this.state.error.toString()}</pre>
      );
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
