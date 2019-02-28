# AMIDOL Front End
## UI/UX Workflow and Specification

Organizational Hierarchy:
* Project
  * Models
  * Composed Models
  * Reward Models
  * Design of Experiments
  * Solver

Basic user workflow begins by **loading** a project.  

## Formal Definition and Terminology

## Example Front End Schema

```json
{ Noun: {
    id: "string",
    model: "filename",
    icon: "filename.svg",
    inputvariables: ["string"],
    outputvariables: ["string"],
    parameters: [{id: "string", val: "expression"}]
  }
}

{ Verb: {
  id: "string",
  model: "filename",
  icon: "filename.svg",
  inputvariables: ["string"],
  outputvariables: ["string"]
  parameters: [{id: "string", val: "expression"}]
  }
}
```

## Model composition schema

```json
{ ComposedModel: {
  id: "string",
  Model0: {
    id: "string"
    sharedVariables: [{
      id: "string"
      }]
    }
  Model1: {
    id: "string"
    sharedVariables: [{
      id: "string"
      }]
    }  
  }
}
```
