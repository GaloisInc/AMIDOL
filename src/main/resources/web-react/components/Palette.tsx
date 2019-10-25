import * as React from "react";
import {
  Input, InputGroup,
  Button, ButtonGroup, ButtonToolbar, Dropdown, DropdownItem, DropdownMenu, DropdownToggle,
  Col, Form, FormGroup, Label, FormText
} from 'reactstrap';
import { PaletteItem, PaletteEditor } from './PaletteEditor.tsx'
import { refillSvg, svgColors } from "../utility/Svg.ts";

export interface PaletteCallbacks {
  // Change the chosen palette
  setChosenPalette(paletteName: string);

  // Change the chosen palette
  getChosenPalette(): string;

  // Add palette group and items
  addPaletteGroup(paletteName: string, items: string[], onComplete: () => void);

  // Fetch the palette item that is selected
  getPaletteItem(name: string): PaletteItem;

  // Fetch the palette item that is selected
  getSelectedPaletteItemName(): string;

  // Set the palette item that is selected
  setSelectedPaletteItemName(name: string): void;
}

interface PaletteItems {
  [key: string]: PaletteItem;
}

interface AvailablePalettes {
  [key: string]: string[];
}

export interface PaletteProps {
  // This must be called whenever some icons have been changed (to update the
  // graph network)
  redrawIcons(): void;
  clearNetwork(reason: string): boolean;
  paletteCallbacks: PaletteCallbacks;
}

export interface PaletteState {
  // Selected palette item
  selected: string;

  // All the palette items
  items: PaletteItems;

  // The palette group chosen
  chosen: string;

  // All the available palette groups
  available: AvailablePalettes;

  // Is the palette editor open?
  paletteEditorOpen: boolean;
}


export class Palette extends React.Component<PaletteProps, PaletteState> {

  constructor(props) {
    super(props);

    this.props.paletteCallbacks.setChosenPalette =
      (paletteName: string) => {
        this.setState({ ...this.state, chosen: paletteName })
      };

    this.props.paletteCallbacks.getChosenPalette =
      () => this.state.chosen;

    this.props.paletteCallbacks.addPaletteGroup =
      (paletteName: string, items: string[], onComplete: () => void) => {
        const newAvailable = { ...this.state.available };
        newAvailable[paletteName] = items

        this.setState({
          ...this.state,
          available: newAvailable,
        });

        this.refreshItems().then(onComplete);
      };

    this.props.paletteCallbacks.getPaletteItem =
      (name: string) => this.state.items[name];

    this.props.paletteCallbacks.getSelectedPaletteItemName =
      () => this.state.selected;

    this.props.paletteCallbacks.setSelectedPaletteItemName =
      (name: string) => this.setState({ ...this.state, selected: name });


    this.state = {
      selected: "population",
      items: { },
      chosen: "SIR",
      available: {
        "SIR": [
    			"population",
    			"patient",
    			"infect",
    			"cure",
    	  ],
        "SIRS-VD": [
    			"population_vital_dynamics",
    			"patient_vital_dynamics",
    			"infect",
    			"cure",
    			"time",
        ],
        "predator_prey": [
    			"predator",
    			"prey",
    			"hunting",
    	  ],
      },
      paletteEditorOpen: false,
    };
  }

  // Pull out the current palette items whose icons need to be rendered
  getCurrentPaletteGroup(): PaletteItem[] {
    return this.state.available[this.state.chosen]
      .map((itemName: string) => this.state.items[itemName])
      .filter((item: PaletteItem) => item !== undefined);
  }

  componentDidMount() {
    this.refreshItems();
  }

  refreshItems(): Promise<void> {

    // Fetch from the server the updated list of palette elements
    return fetch("/appstate/palette/list", { method: "POST" })
      .then(response => response.json())
      .then(json => {
        const items = json as PaletteItem[];
        return Promise.all(items.map(item =>
          refillSvg(item.icon, item.color)
            .then(image => {
                item.image = image;
                return item;
            })
        ));
      })
      .then(items => {
        let itemsObj = { };
        items.forEach(item => itemsObj[item.className] = item);
        this.setState({
          ...this.state,
          items: itemsObj,
        });
      });
  }

  render() {

    const paletteImgs = this.getCurrentPaletteGroup().map((item: PaletteItem) => {
      const setSelected = () => {
        this.setState({ ...this.state, selected: item.className })
      };

      return (
        <div>
          <img
            className={ (item.className == this.state.selected) ? "selected" : "" }
            src={item.image}
            onClick={setSelected}
          />
        </div>
      );
    });

    let paletteEditor;
    if (this.state.paletteEditorOpen) {

      const closeEditor = () => {
        this.setState({
          ...this.state,
          paletteEditorOpen: false,
        });
      };

      const submitPaletteElement = (itemName: string, item: PaletteItem) => {
        let newAvailable = { ...this.state.available };
        if (this.state.items[itemName] === undefined) {
          newAvailable[this.state.chosen] = newAvailable[this.state.chosen].concat(itemName);
        }

        let newItems = { ...this.state.items };
        newItems[itemName] = item;

        this.setState({
          ...this.state,
          available: newAvailable,
          items: newItems,
        });
        this.props.redrawIcons();
      };

      const deletePaletteElement = (itemName: string) => {
        let newAvailable = { };
        Object.keys(this.state.available).forEach(n => {
          if (n != itemName)
            newAvailable[n] = this.state.available[n];
        });

        let newItems = { ...this.state.items };
        delete newItems[itemName];

        this.setState({
          ...this.state,
          available: newAvailable,
          items: newItems,
        });
      };

      paletteEditor = <PaletteEditor
        closeEditor={closeEditor}
        submitPaletteElement={submitPaletteElement}
        deletePaletteElement={deletePaletteElement}

        paletteItems={this.state.items}
        chosenPaletteItemNames={this.state.available[this.state.chosen]}
        editOnOpenItemName={this.state.selected}
      />;
    }

    const openEditor = () => {
      this.setState({ ...this.state, paletteEditorOpen: true })
    };
    const setNewGroup = (e) => {
      const chosen = e.target.value;

      if (this.props.clearNetwork("Changing palettes will clear the drawing canvas.")) {
        this.setState({
          ...this.state,
          chosen,
          selected: this.state.available[chosen][0],
        });
      }
    };

    return (
      <ButtonToolbar>
      <InputGroup>
      <Input
        type="select"
        value={this.state.chosen}
        onChange={setNewGroup}
      >
        {
          Object.keys(this.state.available).map(palette =>
            <option key={palette}>{palette}</option>
          )
        }
      </Input>
      &nbsp;&nbsp;
      <div>
      <Button
        color="secondary"
        onClick={openEditor}
      >
        Edit
      </Button>
        {paletteEditor}
      </div>
      </InputGroup>
        {paletteImgs}
      </ButtonToolbar>
    );
  }

}
