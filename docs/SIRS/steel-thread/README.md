# Steel Thread for SIRS

See [Challenges](challenges.md) for information on modifications needed to address challenges identified during the steel thread exercise for SIRS.

We assume the user builds a model using the nouns and verbs defined in the `VDSOL` directory, and with the models in the `AIR` directory already extant.

* The user adds the following nouns
  * `population.noun` which they name `S`
    * They set `S_P` to `51999999`
  * `population.noun` which they name `I`
    * They set `I_P` to `1`
  * `population.noun` which they name `R`
    * They set `R_P` to `0`
  * `infect.verb` which they name `infect`
    * They set `infect_beta` to `1/3 * 1.24`
    * They set `infect_total_pop` to `S.P + I.P + R.P`
  * `cure.verb` which they name `cure`
    * They set `cure_gamma` to `1/3`

* The user connects the following nouns and verbs:
  * `S` to `infect`
  * `infect` to `I`
  * `I` to `cure`
  * `cure` to `R`

* The backend should take this information and compose the resulting models in the following steps:
  * [Step 0](BackendSteps/step0.json): Generate instance models for all nouns and verbs.
  * [Step 1](BackendSteps/step1.json): Compose `S` and `infect` resulting in `Scinfect`.
  * [Step 2](BackendSteps/step2.json): Compose `infect` with `I`.  Since `infect` has already been composed with `S` resulting in `Scinfect`, we compose `Scinfect` with `I` resulting in `ScinfectcI`.
  * [Step 3](BackendSteps/step3.json): Compose `I` with `cure`.  Since `I` was already composed to form `ScinfectcI`, we compose `ScinfectcI` with `cure` resulting in `ScinfectcIccure`.
  * [Step 4](BackendSteps/step4.json): Compose `cure` with `R`.  Since `cure` was already composed to form `ScinfectcIccure`, we compose `ScinfectcIccure` with `R` resulting in `ScinfectcIccurecR`.

We have some ugly variable names now, but they should work fine for our purposes.  We should keep track of naming and aliases, and do a pass to rename where necessary due to external naming, constants, etc, resulting in [Step 5](BackendSteps/step5.json).

Create the PySCeS model file for the final model, as in [ScinfectcIccurecR.psc](BackendSteps/ScinfectcIccurecR.psc).

* See [tests](tests/) directory for jupyter which tests this PySCeS model.
* **TODO**: Reward variable definition and composing
  * RV composition is easy, just union before the final step of variable renaming (step 5 above).
  * **TODO**: Translating reward variables to PySCeS.
    * Basic sketch - we need to generate a small python script to solve the PySCeS model file, and following the form provided by the [PySCeS Manual](http://pysces.sourceforge.net/docs/userguide_doc.html#time-simulation) we need to add:

```python
mod.mode_integrator = 'LSODA'

mod.sim_start = 0.0
mod.sim_end = 20
mod.sim_points = 50
mod.Simulate()

data_sim.getSpecies(lbls=True)
```

Where we set `sim_start`, `sim_end`, and `sim_points` appropriately so they return the temporal characteristics of the reward variables.  We extract the necessary rate rewards from the output of `getSpecies()`, and then solve for composed reward variable expressions.
