import * as React from "react";
import { Button, ButtonToolbar } from 'reactstrap';

import { ModelNavigation, ModelNavigationProps } from "./ModelNavigation"
import { ExecuteButton, ExecuteButtonProps } from "./ExecuteButton"


interface ButtonBarProps extends ExecuteButtonProps, ModelNavigationProps {
  onJuliaUpload();
}

// The full button bar at the top of the AMIDOL UI
export class ButtonBar extends React.Component<ButtonBarProps, {}> {
  render() {
    return (
      <ButtonToolbar>
        <Button
          onClick={this.props.onJuliaUpload}
          outline color="secondary"
        >Load Julia code</Button>
        &nbsp;&nbsp;

        <ModelNavigation 
          journal={this.props.journal}
          onUpload={this.props.onUpload}
          onDownload={this.props.onDownload}
        />
        &nbsp;&nbsp;

        <ExecuteButton
          setNewCurrentBackend={this.props.setNewCurrentBackend}
          onExecute={this.props.onExecute}
        />
        &nbsp;&nbsp;

      </ButtonToolbar>
    );
  }
}
  
