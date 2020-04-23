This is an example of a more complex SEIR model with two diseases that have some
cross-immunity and a `β` that changes in a seasonal fashion. This is an attempt
to reproduce [this paper][0] (DOI: 10.1126/science.abb5793).

## Optional setup

`beta.json` and `covid_equations.txt` are committed, but sometimes you may want
to modify how they are generated.

  1. Regenerate `beta.json`. This represents the values that `β` takes every day
     for 5 years, using a periodic sinusoidal (to account for seasonal changes)

     ```sh
     $ python3 covid_beta.py
     ```

  2. Regenerate `covid_equations.txt`.

     ```sh
     $ python3 covid_composition.py > covid_equations.txt
     ```

## Steps to run

  1. Start AMIDOL and open the `diff-eq` (<http://localhost:8080/diff-eq.html>)
     and `compare` (<http://localhost:8080/compare.html>) pages

  2. In the `compare` page, under the "Upload a new data trace", enter the name
     `beta` and the select the newly generated `beta.json` file, then click
     "Upload series"

  3. In the `diff-eq` page, copy paste the contents of `covid_equations.txt`
     into the main textbox, then adjust the time parameters to be:

       * Start: 0
       * Until: 1825
       * Step: 0.5

     Then click "Simulate"

  4. Back in the `compare` page, you can tally up the total infected by making
     a new series ("Add new" button) called "total infected" and then adding
     each of the 7 data traces that represent infected states.

[0]: https://science.sciencemag.org/content/early/2020/04/14/science.abb5793.full
