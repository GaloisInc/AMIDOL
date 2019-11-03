import * as React from "react";
import {
  Button, Input, Col, Form, FormGroup, Label, Row,
  ListGroup, ListGroupItem, ListGroupItemHeading
} from 'reactstrap';

export interface Property {
  name: string,
  value: string,
}

export interface VariablesCallbacks {
  setVariables(nodeId: string, nodeLabel: string, properties: Property[]);
  unsetVariables();
}

export interface LocalVariables {
  nodeId?: string;
  nodeLabel?: string;
  properties?: Property[];
}

interface VariablesProps {
  updateNodeProperty(nodeId: string, property: Property): void;

  variablesCallbacks: VariablesCallbacks;
}

export class Variables extends React.Component<VariablesProps, LocalVariables> {

  constructor(props) {
    super(props);

    this.state = {};

    this.props.variablesCallbacks.setVariables =
      (nodeId: string, nodeLabel: string, properties: Property[]) => {
        this.setState({ nodeId, nodeLabel, properties });
      };
    this.props.variablesCallbacks.unsetVariables =
      () => {
        this.setState({ });
      };
  }

  render() {
    if (!this.state.nodeId || !this.state.properties || !this.state.nodeLabel) {
      return null;
    }

    return (
      <div>
        <h2>{this.state.nodeLabel}</h2>
          {
            this.state.properties.map((property: Property, i: number) => {
              const updateSingle = (newValue: string) => {
                const newProperties = this.state.properties.map((p, j) =>
                  (i == j) ? { ...p, value: newValue } : p
                );
                this.props.updateNodeProperty(this.state.nodeId, { ...property, value: newValue });
                this.setState({ ...this.state, properties: newProperties });
              }
              return <Variable
                property={property}
                updateSingleProperty={updateSingle}
              />;
            })
          }
      </div>
    );
  }

}

interface VariableProps {
  property: Property;
  updateSingleProperty(newValue: string);
}

export class Variable extends React.Component<VariableProps, {}> {

  render() {
    return (
      <FormGroup row>
        <Label for={this.props.property.name} sm={4}>
          {this.props.property.name}
        </Label>
        <Col sm={8}>
          <Input
            id={this.props.property.name}
            value={this.props.property.value}
            onChange={(e) => this.props.updateSingleProperty(e.target.value)}
          />
        </Col>
      </FormGroup>
    );
  }
}
