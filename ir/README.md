### Building

You will need to have:
  - a recent version of the Oracle [JDK] version 8 (http://www.oracle.com/technetwork/java/javase/downloads/index.html)
  - the latest version of [SBT](http://www.scala-sbt.org/) installed and on your path

Then, you can use (from this directory)

    $ sbt compile    # ... to compile
    $ sbt run        # ... to compile and run
    $ sbt test       # ... to compile and run tests

### Sample queries

Executed from this directory with the backend running (ie: `sbt run`):

  * Get the model currently loaded:

    ```sh
    curl -H 'Accept: application/json' -X GET \
      'http://localhost:8080/appstate/model'
    ```
  
  * Update the loaded model (to be `src/main/resources/sirs_graph.json`):

    ```sh
    curl -H 'Content-Type: application/json' -X POST \
      -d @src/main/resources/sirs_graph.json \
      'http://localhost:8080/appstate/model'
    ```
  
  * Solve the IVP using Python's `odeint`:

    ```sh
    curl -H 'Accept: application/json' -X GET \
      'http://localhost:8080/integrateDemo?constants=\{"N":52000000,"%CE%B3":0.333,"%CE%B2":0.413,"%CE%BC":0\}&boundary=\{"Susceptible":51999999,"Infectious":1,"Recovered":0\}&initialTime=40&finalTime=100&stepSize=1'
    ```
