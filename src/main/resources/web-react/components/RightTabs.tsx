import * as React from "react";
import { TabContent, TabPane, Nav, NavItem, NavLink, Card, Button, CardTitle, CardText, Row, Col } from 'reactstrap';
import classnames from 'classnames';

import { Measures, MeasuresCallbacks, MeasureProps } from "./RewardVariables";
import { Variables, VariablesCallbacks, VariablesProps, Property } from "./Variables";

interface TabsProps {
  updateNodeProperty: (nodeId: string, property: Property) => void;
  setMeasure: (name: string, oldP: MeasureProps, newP: MeasureProps) => void; 
  callbacks: MeasuresCallbacks & VariablesCallbacks;
}

export class RightTabs extends React.Component<TabsProps, { activeTab: string }> {

  constructor(props) {
    super(props);

    this.state = {
      activeTab: '1',
    };
  }
  
  render() {

    const toggle = tab => {
      if (this.state.activeTab !== tab) this.setState({ ...this.state, activeTab: tab });
    };

    return (
      <div>
        <Nav tabs>
          <NavItem>
            <NavLink
              className={classnames({ active: this.state.activeTab === '1' })}
              onClick={() => { toggle('1'); }}
            >
              Measures
            </NavLink>
          </NavItem>
          <NavItem>
            <NavLink
              className={classnames({ active: this.state.activeTab === '2' })}
              onClick={() => { toggle('2'); }}
            >
              Parameters
            </NavLink>
          </NavItem>
        </Nav>
        <TabContent activeTab={this.state.activeTab}>
        <TabPane tabId="1">
          <Measures
            callbacks={this.props.callbacks}
            setMeasure={this.props.setMeasure}
          />
        </TabPane>
        <TabPane tabId="2">
          <Variables
            variablesCallbacks={this.props.callbacks}
            updateNodeProperty={this.props.updateNodeProperty}
          />
        </TabPane>
      </TabContent>
      </div>
    );
  }

}
