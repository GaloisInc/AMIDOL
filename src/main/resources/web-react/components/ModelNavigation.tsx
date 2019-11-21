import * as React from "react";
import {
  Button, ButtonGroup, ButtonToolbar, Dropdown, DropdownItem, DropdownMenu, DropdownToggle,
  Col, Form, FormGroup, Label, FormText
} from 'reactstrap';


export interface ModelNavigationState {
  canUndo: boolean;
  canRedo: boolean;
}

export interface Journal {
  past: JournalEvent[];
  future: JournalEvent[];
  
  // Given an event, apply it to the external world
  applyEventEffects: (event: JournalEvent) => void;

  // How to invert an event
  invertEvent: (event: JournalEvent) => JournalEvent;

  // Filled in here!
  apply?: (event: JournalEvent) => void;

  // Filled in here!
  // Keep redo-ing until there is nothing to redo
  redoAll?: () => void;
}

// TODO do more here?
type JournalEvent = any;

export interface ModelNavigationProps {
  journal: Journal;
  onUpload: () => void;
  onDownload: () => void;
}


// Load model, download model, undo, redo
export class ModelNavigation extends React.Component<ModelNavigationProps, ModelNavigationState> {

  constructor(props) {
    super(props);

    this.state = {
      canRedo: false,
      canUndo: false,
    };

    this.undo = this.undo.bind(this);
    this.redo = this.redo.bind(this);
    this.reset = this.reset.bind(this);

    this.props.journal.apply = this.apply.bind(this);
    this.props.journal.redoAll = this.redoAll.bind(this);
  }

  // Apply an event
  apply(event: JournalEvent) {
   // Adjust the journal
   this.props.journal.past.push(event);
   this.props.journal.future = [];

   // Apply the effect
   this.props.journal.applyEventEffects(event);

   // Update the undo/redo button state
   this.setState({
     ...this.state,
     canRedo: false,
     canUndo: true,
   });
  }

  // Move an event from the end of the past to the beginning of the future
  undo() {
    const pastLength = this.props.journal.past.length;
    if (pastLength > 0) {
      const lastEvent = this.props.journal.past.pop();
      this.props.journal.future.unshift(lastEvent);

      // Apply the inverse effect to undo the last event
      const invertedEvent = this.props.journal.invertEvent(lastEvent);
      this.props.journal.applyEventEffects(invertedEvent);

      // Update undo/redo button state
      this.setState({
        ...this.state,
        canRedo: true,
        canUndo: pastLength > 1,
      });
    }
  }

  // Move an event from the beginning of the future to the end of the past
  redo() {
    const futureLength = this.props.journal.future.length;
    if (futureLength > 0) {
      const nextEvent = this.props.journal.future.shift();
      this.props.journal.past.push(nextEvent);

      // Apply the future effect
      this.props.journal.applyEventEffects(nextEvent);

      // Update undo/redo button state
      this.setState({
        ...this.state,
        canRedo: futureLength > 1,
        canUndo: true,
      });
    }
  }

  // Continue redo-ing until there is nothing to redo
  redoAll() {
    this.props.journal.future.forEach(this.props.journal.applyEventEffects);
    this.props.journal.past = this.props.journal.future;
    this.props.journal.future = [];

    // Update undo/redo button state
    this.setState({
      ...this.state,
      canRedo: false,
      canUndo: this.props.journal.past.length > 0,
    });
  }

  // Reset all appstate!
  reset() {
		if (confirm("WARNING: This is going to clear all the internal application state")) {
			fetch("/appstate/reset", { method: "POST" })
        .then(() => { location.reload(); });
		}
  }

  render() {
    return (
      <ButtonGroup>
        <Button
          id="undo_btn" 
          title="Undo"
          disabled={!this.state.canUndo}
          outline
          color="secondary"
          onClick={this.undo}
        ><i className="ion ion-ios-undo"></i></Button>
        <Button
          id="download_btn"
          title="Save to a model file"
          outline color="secondary"
          onClick={this.props.onDownload}
        ><i className="ion ion-ios-cloud-download"></i></Button>
        <Button
          id="upload_btn"
          title="Load a model file"
          outline color="secondary"
          onClick={this.props.onUpload}
        ><i className="ion ion-ios-cloud-upload"></i></Button>
        <Button
          id="reset_btn"
          title="Reset"
          outline color="danger"
          onClick={this.reset}
        ><i className="ion ion-power"></i></Button>
        <Button
          id="redo_btn"
          title="Redo"
          disabled={!this.state.canRedo}
          outline
          color="secondary"
          onClick={this.redo}
        ><i className="ion ion-ios-redo"></i></Button>
      </ButtonGroup>
    );
  }
}

