# Steel Thread for SIRS

See [Challenges](challenges.md) for information on modifications needed to address challenges identified during the steel thread exercise for SIRS.

We assume the user builds a model using the nouns and verbs defined in the `VDSOL` directory, and with the models in the `AIR` directory already extant.

* The user adds the following nouns
  * `population.noun` which they name `S`
    * They set `S.P` to `51999999`
  * `population.noun` which they name `I`
    * They set `I.P` to `1`
  * `population.noun` which they name `R`
    * They set `R.P` to `0`
  * `infect.verb` which they name `infect`
    * They set `infect.beta` to `1/3 * 1.24`
    * They set `infect.total_pop` to `S.P + I.P + R.P`
  * `cure.verb` which they name `cure`
    * They set `cure.gamma` to `1/3`

* The user connects the following nouns and verbs:
  * `S` to `infect`
  * `infect` to `I`
  * `I` to `cure`
  * `cure` to `R`

* The backend should take this information and compose the resulting models in the following steps:
  * [Step 0](BackendSteps/step0.json): Generate instance models for all nouns and verbs.
