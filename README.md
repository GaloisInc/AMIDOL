# AMIDOL

The UI (an Elm project contained in the `ui` directory) is a series of
HTML pages through which users will interact with the system. That includes
drawing/manipulating semi-formal models, issuing queries, loading data.

The main system (a Scala project contained in the `ir` directory) compiles
questions that users may have about models they've created into code artifacts
targeting different simulation/solver backends. Over time, we expect this
system to also manipulate and compose datasets (from real-world observations
as well as from the output of other simulations).

### Building

You'll need to:

  * install the [`elm` compiler][0] for building the `ui`

  * install the [`sbt` build tool][1] and a recent version of the
    [Oracle JDK][2] for building the `ir`

  * install the [`python3` interpreter][3] and [`pip3` package manager][4],
    then the `scipy` and `matplotlib` packages (by doing
    `pip3 install scipy matplotlib`) for the SciPy backends to the `ir`

Once you have done all of this, build with: 

```sh
$ git clone https://github.com/GaloisInc/AMIDOL.git && cd AMIDOl
AMIDOL$ (cd ui; elm make src/Main.elm)    # builds ui/index.html
AMIDOL$ (cd ir; sbt compile)              # builds the main system
```

To run the system and open a webserver on <http://localhost:8080/>:

```sh
AMIDOL$ (cd ir; sbt run)                  # builds and runs the main system
```


[0]: https://guide.elm-lang.org/install.html
[1]: https://www.scala-sbt.org/download.html
[2]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[3]: https://www.python.org/downloads/
[4]: https://pip.pypa.io/en/stable/installing/
