import * as React from "react";
import {
  Input, InputGroup,
  Button, ButtonGroup, ButtonToolbar, Dropdown, DropdownItem, DropdownMenu, DropdownToggle,
  Col, Form, FormGroup, Label, FormText
} from 'reactstrap';


interface Dictionary<T> {
    [key: string]: T;
}

export interface PaletteState {
  availablePalettes: { name: string, elements: string[] }[];
  selectedPalette: string;
  paletteItems: any; // TODO
}


export class Palette extends React.Component<{}, PaletteState> {

  constructor(props) {
    super(props);

    this.state = {
      selectedPalette: "SIR",
      availablePalettes: [
    		{
          name: "SIR",
          elements: [
    				"population",
    				"patient",
    				"infect",
    				"cure",
    		  ]
        }, {
    		  name: "SIRS-VD",
          elements: [
    				"population_vital_dynamics",
    				"patient_vital_dynamics",
    				"infect",
    				"cure",
    				"time",
          ]
    		}, {
    		  name: "predator_prey",
          elements: [
    				"predator",
    				"prey",
    				"hunting",
    		  ],
        }
    	],
      paletteItems: { },
    };
  }

  render() {
    return (
      <ButtonToolbar>
<div style={{ width: '200px' }}>
      <InputGroup>
      <Input
        type="select"
        value={this.state.selectedPalette}

      >
        {
          this.state.availablePalettes.map(palette =>
            <option>{palette.name}</option>
          )
        }
      </Input>
      &nbsp;&nbsp;

      <Button
        color="secondary"
      >
        Edit
      </Button>
      </InputGroup>
      </div>
      </ButtonToolbar>
    );
  }

}
