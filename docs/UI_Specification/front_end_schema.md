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

## Frontend Output schema

```json
{ "nouns": ["noun1.noun", "noun2.noun", ...],
  "verbs": ["verb1.verb", "verb2.verb", ...],
  "edges": [{"source": "noun1.noun", "sink": "verb1.verb"}, ...]
}

//Nouns

//Verbs
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
