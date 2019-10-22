import * as React from "react";
import {
  Button, ButtonGroup, ButtonToolbar, ButtonDropdown, DropdownItem, DropdownMenu, DropdownToggle,
  Col, Form, FormGroup, Label, FormText
} from 'reactstrap';


export interface ExecuteButtonProps {
  setNewCurrentBackend(newBackend: string);
  onExecute();
}

export interface ExecuteButtonState {
  currentBackend: string;
  dropdownOpen: boolean;
}

// Execute the model, and the backend of choice
export class ExecuteButton extends React.Component<ExecuteButtonProps, ExecuteButtonState> {
  
  constructor(props) {
    super(props);

    this.state = {
      currentBackend: "SciPy",
      dropdownOpen: false,
    };

    this.toggleDropdown = this.toggleDropdown.bind(this);
    this.setBackend = this.setBackend.bind(this);
  }

  // Toggle the dropdown open state
  toggleDropdown() {
    this.setState({ ...this.state, dropdownOpen: !this.state.dropdownOpen });
  }

  setBackend(newBackend: string) {
    this.setState({ dropdownOpen: false, currentBackend: newBackend });
    this.props.setNewCurrentBackend(newBackend.toLowerCase());
  }

  render() {
    return (
      <ButtonGroup>
        <Button 
         id="execute_btn"
         title="Run the model"
         outline color="primary"
         onClick={this.props.onExecute}
        >Execute</Button>
        <ButtonDropdown color="primary" isOpen={this.state.dropdownOpen} toggle={this.toggleDropdown}>
          <DropdownToggle caret color="primary">
            <span>on {this.state.currentBackend} backend</span>
          </DropdownToggle>
          <DropdownMenu>
            <DropdownItem><div onClick={() => this.setBackend("Julia")}>Julia</div></DropdownItem>
            <DropdownItem><div onClick={() => this.setBackend("SciPy")}>SciPy</div></DropdownItem>
            <DropdownItem><div onClick={() => this.setBackend("PySCeS")}>PySCeS</div></DropdownItem>
          </DropdownMenu>
        </ButtonDropdown>
      </ButtonGroup>
    );
  }
}

