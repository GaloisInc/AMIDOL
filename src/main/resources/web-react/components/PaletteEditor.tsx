import * as React from "react";
import {
  Button, Modal, ModalHeader, ModalBody, ModalFooter,
  Dropdown, DropdownItem, DropdownMenu, DropdownToggle,
  Input, InputGroupAddon, InputGroup, InputGroupText, InputGroupButtonDropdown,
  Col, Form, FormGroup, Label, FormText
} from 'reactstrap';

export interface PaletteEditorState {
  name: string;
  'type': string; // noun or verb
  icon: string;
  color: string;
  sharedStates: string[];
  backingModel: string;

  isOpen: boolean;
}

export interface PaletteEditorProps {
  // exit editor without doing anything
  closeEditor(): void;

  // effect change on global palette
  submitPaletteElement(itemName: string, item: PaletteItem, onSuccess: () => void): void;

  // remove existing palette element
  deletePaletteElement(itemName: string, onSuccess: () => void): void;

  paletteItems: { [itemName: string]: PaletteItem };
  chosenPaletteItemNames: string[];

  editOnOpenItemName?: string;    // which palette item to load in
}

export interface PaletteItem {
  className: string;
  'type': string,
  sharedStates: string[],

  icon: string,
  color: string, // TODO: optional?
  backingModel: any
}

// Palette editor modal
export class PaletteEditor extends React.Component<PaletteEditorProps, PaletteEditorState> {

  constructor(props) {
    super(props);

    this.state = {
      name: "",
      'type': "noun",
      icon: "",
      sharedStates: [],
      backingModel: "",
      color: "",
      isOpen: true,
    };

    this.toggle = this.toggle.bind(this);
    this.updateName = this.updateName.bind(this);
    this.updateTemplate = this.updateTemplate.bind(this);
    this.updateType = this.updateType.bind(this);
    this.updateIcon = this.updateIcon.bind(this);
    this.updateSharedStates = this.updateSharedStates.bind(this);
    this.updateBackingModel = this.updateBackingModel.bind(this);
  }

  componentDidMount() {
    this.updateTemplate(this.props.editOnOpenItemName);
  }

  updateName(newName: string) {
    this.setState(prevState => ({ ...prevState, name: newName }));
  }

  updateTemplate(newName: string) {
    if (!newName) {
      this.setState(prevState => ({
        ...prevState,
        name: "",
        'type': "noun",
        icon: "",
        sharedStates: [],
        backingModel: "",
        color: "",
      }));
    } else {
      const paletteItem: PaletteItem = this.props.paletteItems[newName];
      this.setState(prevState => ({
        ...prevState,
        name: newName,
        'type': paletteItem.type,
        icon: paletteItem.icon,
        sharedStates: paletteItem.sharedStates,
        backingModel: JSON.stringify(paletteItem.backingModel, null, 2),
        color: paletteItem.color
      }));
    }
  }

  updateType(newType: string) {
    this.setState(prevState => ({ ...prevState, 'type': newType }));
  }

  updateIcon(newIcon: string, newColor: string) {
    this.setState(prevState => ({ ...prevState, icon: newIcon, color: newColor }));
  }

  updateSharedStates(newStates: string[]) {
    this.setState(prevState => ({ ...prevState, sharedStates: newStates }));
  }

  updateBackingModel(newModel: string) {
    this.setState(prevState => ({ ...prevState, backingModel: newModel }));
  }

  toggle() {
    this.setState(prevState => ({ ...prevState, isOpen: !prevState.isOpen }));
  }

  currentPaletteItem() {
    if (!this.state.name) return undefined;
    return {
      className: this.state.name,
      'type': this.state['type'],
      sharedStates: this.state.sharedStates,
      icon: this.state.icon,
      color: this.state.color,
      backingModel: JSON.parse(this.state.backingModel)
    };
  }

  render() {
    const updating = this.props.chosenPaletteItemNames.indexOf(this.state.name) > -1;
    var confButtons = [];
    var wip: PaletteItem = undefined;
    try {
      wip = this.currentPaletteItem()
    } catch (e) { }

    if (updating) {
      confButtons = [
        <Button
          color="primary"
          onClick={() => this.props.deletePaletteElement(
            this.state.name,
            this.toggle
          )}
        >
          Remove element
        </Button>,
        <Button
          color="primary"
          onClick={() => this.props.submitPaletteElement(
            this.state.name, wip,
            this.toggle
          )}
          disabled={wip === undefined}
        >
          Update existing element
        </Button>
      ];
    } else {
      confButtons = [
        <Button
          color="primary"
          onClick={() => this.props.submitPaletteElement(
            this.state.name, wip,
            this.toggle
          )}
          disabled={wip === undefined}
        >
          Create new element
        </Button>
      ];
    }

    return (
      <Modal
        isOpen={this.state.isOpen}
        onClosed={this.props.closeEditor}
        toggle={this.toggle}
        size={"lg"}
      >
        <ModalHeader toggle={this.toggle}>
          Palette element editor
        </ModalHeader>
        <ModalBody>
          <Form>
            <NameDropdown
              updateTemplate={this.updateTemplate}
              updateName={this.updateName}
              templates={this.props.chosenPaletteItemNames}
              currentName={this.state.name}
            />
            <TypeDropdown
              updateType={this.updateType}
              currentType={this.state['type']}
            />
            <IconPicker
              updateIcon={this.updateIcon}
              currentIcon={this.state.icon}
              currentColor={this.state.color}
            />
            <SharedStatesInput
              updateSharedStates={this.updateSharedStates}
              currentSharedStates={this.state.sharedStates}
            />
            <BackingModelInput
              updateBackingModel={this.updateBackingModel}
              currentBackingModel={this.state.backingModel}
            />
          </Form>
        </ModalBody>
        <ModalFooter>
          {confButtons}
          <Button color="secondary" onClick={this.toggle}>Cancel</Button>
        </ModalFooter>
      </Modal>
    );
  }
}


interface NameDropdownProps {
  updateTemplate(newName: string): void;
  updateName(newName: string): void;

  templates: string[];
  currentName: string;
}

interface NameDropdownState {
  dropdownOpen: boolean;
}

class NameDropdown extends React.Component<NameDropdownProps, NameDropdownState> {
  constructor(props) {
    super(props);

    this.toggleDropDown = this.toggleDropDown.bind(this);
    this.state = {
      dropdownOpen: false
    };
  }

  toggleDropDown() {
    this.setState(prevState => ({
      dropdownOpen: !prevState.dropdownOpen
    }));
  }

  render() {
    return (
      <FormGroup row>
        <Label sm={2} for="elementName">Name</Label>
        <Col sm={10}>
          <InputGroup>
            <Input
              onChange={(e) => this.props.updateName(e.target.value)}
              value={this.props.currentName}
              placeholder="name of palette element to edit or create"
              id="elementName"
            />
            <InputGroupButtonDropdown
              addonType="append"
              isOpen={this.state.dropdownOpen}
              toggle={this.toggleDropDown}
            >
              <DropdownToggle caret>
                Load existing element
              </DropdownToggle>
              <DropdownMenu>
                {
                  this.props.templates.map(template => (
                    <DropdownItem onClick={() => this.props.updateTemplate(template)}>
                      {template}
                    </DropdownItem>
                  ))
                }
                <DropdownItem divider />
                <DropdownItem onClick={() => this.props.updateTemplate(undefined)}>
                  New element
                </DropdownItem>
              </DropdownMenu>
            </InputGroupButtonDropdown>
          </InputGroup>
        </Col>
      </FormGroup>
    );
  }
}


interface TypeDropdownProps {
  updateType(newType: string): void;
  currentType: string;
}

class TypeDropdown extends React.Component<TypeDropdownProps, {}> {
  render() {
    return (
      <FormGroup row>
        <Label for="nounOrVerbSelect" sm={2}>Type</Label>
        <Col sm={10}>
          <Input
            type="select"
            name="select"
            id="nounOrVerbSelect"
            value={this.props.currentType}
            onChange={(e) => this.props.updateType(e.target.value)}
          >
            <option value="noun">Noun</option>
            <option value="verb">Verb</option>
          </Input>
        </Col>
      </FormGroup>
    );
  }
}


interface IconPickerProps {
  updateIcon(newIcon: string, newColor: string): void;
  currentIcon: string;
  currentColor: string;
}

class IconPicker extends React.Component<IconPickerProps, {}> {
  render() {
    return (
      <FormGroup row>
        <Label for="iconSelect" sm={2}>Icon</Label>
        <Col sm={8}>
          <Input
            type="select"
            name="select"
            id="iconFormat"
            value={this.props.currentIcon}
            onChange={(e) => this.props.updateIcon(
              e.target.value,
              this.props.currentColor
            )}
          >
            <option value="images/birth.svg">Birth</option>
            <option value="images/breeding.svg">Breeding</option>
            <option value="images/cure.svg">Cure</option>
            <option value="images/death.svg">Death</option>
            <option value="images/hunting.svg">Hunting</option>
            <option value="images/patient.svg">Patient</option>
            <option value="images/person.svg">Person</option>
            <option value="images/predator.svg">Predator</option>
            <option value="images/prey.svg">Prey</option>
            <option value="images/time.svg">Time</option>
            <option value="images/virus.svg">Virus</option>
            <option value="images/Ebola.svg">Ebola</option>
            <option value="images/HIV.svg">HIV</option>
          </Input>
        </Col>
        <Col sm={2}>
          <Input
            type="color"
            name="color"
            value={this.props.currentColor}
            onChange={(e) => this.props.updateIcon(
              this.props.currentIcon,
              e.target.value,
            )}
          />
        </Col>
      </FormGroup>
    );
  }
}


interface SharedStatesInputProps {
  updateSharedStates(newStates: string[]): void;
  currentSharedStates: string[];
}

class SharedStatesInput extends React.Component<SharedStatesInputProps, {}> {
  render() {
    return (
      <FormGroup row>
        <Label for="sharedStatesInput" sm={2}>Shared states</Label>
        <Col sm={10}>
          <Input
            id="sharedStatesInput"
            placeholder="comma-delimited shared states"
            value={this.props.currentSharedStates.join()}
            onChange={(e) => this.props.updateSharedStates(e.target.value.split(','))}
          />
        </Col>
      </FormGroup>
    );
  }
}


interface BackingModelInputProps {
  updateBackingModel(newModel: string): void;
  currentBackingModel: string;
}

class BackingModelInput extends React.Component<BackingModelInputProps, {}> {
  render() {
    var currentModelIsMalformed: boolean = false;
    try {
      JSON.parse(this.props.currentBackingModel);
    } catch (e) {
      currentModelIsMalformed = true;
    }

    return (
      <FormGroup row>
        <Label for="backingModelInput" sm={2}>Backend model</Label>
        <Col sm={10}>
          <Input
            type="textarea"
            rows="10"
            id="backingModelInput"
            placeholder="JSON for backend model"
            value={this.props.currentBackingModel}
            onChange={(e) => this.props.updateBackingModel(e.target.value)}
            invalid={currentModelIsMalformed}
          />
        </Col>
      </FormGroup>
    );
  }
}




