# AMIDOL

AMIDOL is designed to support models in a number of scientific, physical, social, and hybrid domains by
allowing domain experts to construct meta-models in a novel way, using visual domain specific
ontological languages (VDSOLs). These VDSOLs utilize an underlying intermediate abstract
representation to give formal meaning to the intuitive process diagrams scientists and domain
experts normally create. AMIDOL then provides translations from these VDSOLs into an intermediate
representation which can be transformed as appropriate to compose models, apply optimizations, and
translate them into executable representations allowing AMIDOL's inference engine to execute
prognostic queries on reward models and communicate results to domain experts.

### Building

You'll need to:

  * install the [`sbt` build tool][1] and a recent version of the
    [Oracle JDK][2] for building the `ir`

  * install the [`python` interpreter][3] and [`pip` package manager][4],
    then the `pysces` and `numpy` packages (by doing
    `pip install pysces numpy`) for the PySCeS backend

  * install the [`python3` interpreter][3] and [`pip3` package manager][4],
    then the `scipy` and `matplotlib` packages (by doing
    `pip3 install scipy matplotlib`) for the SciPy backends

Once you have done all of this, build and run the system with: 

```sh
$ git clone https://github.com/GaloisInc/AMIDOL.git && cd AMIDOl
AMIDOL$ sbt run 
```

This opens a back-end web server on http://localhost:8080/ . NOTE: This system was only meant for use/tested on
Google Chrome. This version of the system does not support other browsers.

Example models can be found in this repository under the `examples` directory. These are JSON 
files meant to be loaded into the web browser UI, with the cloud-shaped upload button in the 
upper-right. User-drawn models can also be downloaded with the adjacent download button.


[1]: https://www.scala-sbt.org/download.html
[2]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[3]: https://www.python.org/downloads/
[4]: https://pip.pypa.io/en/stable/installing/
