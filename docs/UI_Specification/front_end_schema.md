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
{ "noun": {
    "className": "string",
    "classDef": "filename",
    "icon": "filename.svg",
    "inputVariables": ["string"],
    "outputVariables": ["string"],
    "parameters": [{"name": "string", "value": "expression"}]
  }
}

{ "verb": {
  "className": "string",
  "classDef": "filename",
  "icon": "filename.svg",
  "inputVariables": ["string"],
  "outputVariables": ["string"],
  "parameters": [{"name": "string", "value": "expression"}]
  }
}
```

## Model composition schema

```json
{ "composedmodel": {
  "id": "string",
  "model0": {
    "id": "string"
    "sharedvariables": [{
      "id": "string"
      }]
    }
  "model1": {
    "id": "string"
    sharedVariables: [{
      id: "string"
      }]
    }  
  }
}
```
