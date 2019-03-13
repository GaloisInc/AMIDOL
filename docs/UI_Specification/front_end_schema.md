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
    "id": "string",
    "model": "filename",
    "icon": "filename.svg",
    "inputvariables": ["string"],
    "outputvariables": ["string"],
    "parameters": [{"id": "string", "val": "expression"}]
  }
}

{ "verb": {
  "id": "string",
  "model": "filename",
  "icon": "filename.svg",
  "inputvariables": ["string"],
  "outputvariables": ["string"],
  "parameters": [{"id": "string", "val": "expression"}]
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
