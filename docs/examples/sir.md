---
layout: post
title: SIR Family of Models
toc: true
---

## General Methodology


## SIR

$$ \frac{dS}{dt} = -\frac{\beta IS}{N} $$

$$ \frac{dI}{dt} = \frac{\beta IS}{N} - \gamma I $$

$$ \frac{dR}{dt} = \gamma I $$

```json
{ "model": {
  "name": ["SIR Model"],
  "state_variables": [
    { "state_variable": { "name": ["S"], "labels": [], "type": "int",
    "initial_value": "51999999"}},
    { "state_variable": { "name": ["I"], "labels": [], "type": "int",
    "initial_value": "1"}},
    { "state_variable": { "name": ["R"], "labels": [], "type": "int",
    "initial_value": "0"}}
  ],
  "events": [
    { "event": {"name": ["infect"], "labels": [], "rate": "beta * S * I / N",
      "input_predicate": { "enabling_condition": "(N > 0) AND (beta * S * I / N > 0)"},
      "output_predicate": {"transition_function": [{"sv_name": "S", "function": "-1"}, {"sv_name": "I", "function": "1"}]},
    }},
    { "event": {"name": ["recover"], "labels": [], "rate": "gamma * I",
      "input_predicate": { "enabling_condition": "(gamma * I > 0)"},
      "output_predicate": {"transition_function": [{"sv_name": "I", "function": "-1"}, {"sv_name": "R", "function": "1"}]},
    }}
  ],
  "constants": [{ "constant": {"name": "N", "value": "52000000"}}, { "constant": {"name": "beta",   
    "value": "0.41333"}}, { "constant": {"name": "gamma", "value": "0.33333"}}
  ],
  "rate_rewards": [{ "rate_reward": {"name": "Cumulative Infected", "sv_name": "I",
    "reward": "I", "temporal_type": "interval_of_time", "temporal_domain": ["40, 100, 1"]}}],
  "impulse_rewards": [],
  "composed_rewards": []
  }
}
```

## SEIR

$$ \frac{dS}{dt} = \mu N - \mu S -\frac{\beta IS}{N} $$

$$ \frac{dE}{dt} = \frac{\beta IS}{N} - \mu E - \alpha E $$

$$ \frac{dI}{dt} = \alpha E - \gamma I - \mu I$$

$$ \frac{dR}{dt} = \gamma I - \mu R$$
