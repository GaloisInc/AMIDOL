import numpy as np
import itertools
import math
from tqdm import tqdm_notebook
import pandas as pd

def metasolve(SV, SV0, C, CVals, Phi, Delta, Lambda, Measures, t0=0.0, tMax=100.0, runs=100):
    FullMeasures = [(x,list(np.arange(y[0], y[1]+y[2], y[2]))) for x,y in Measures]
    Means = {x:0.0 for x in list(itertools.chain(*[zip(itertools.cycle(x), y) for x,y in FullMeasures]))}
    Variances = {x:0.0 for x in list(itertools.chain(*[zip(itertools.cycle(x), y) for x,y in FullMeasures]))}
    Samples = {x:0 for x in list(itertools.chain(*[zip(itertools.cycle(x), y) for x,y in FullMeasures]))}
    ConfInterval = {x:0.0 for x in list(itertools.chain(*[zip(itertools.cycle(x), y) for x,y in FullMeasures]))}

    namespace = {}

    for run in (range(0, runs)):

        # Initialize the model
        for statevar, val in zip(SV, SV0):
            namespace[statevar] = eval(val, namespace)

        for constant, val in zip(C, CVals):
            namespace[constant] = eval(val, namespace)

        t = t0
        enabled = [(eval(x, namespace) and (eval(y, namespace) > 0.0)) for x,y in zip(Phi, Lambda)]
        while((t < tMax) and any(enabled)):
            # Find the currently enabled events, sum their total rate
            currentRates = [eval(x, namespace) if y else float(0) for x,y in zip(Lambda, enabled)]
            totalRate = sum(currentRates)
            # Convert to biases for the discrete distribution selecting the rate
            biases = [x/totalRate for x in currentRates]
            nextEventIdx = np.random.choice(len(Lambda), p=biases)
            nextDelay = np.random.exponential(1.0/(totalRate))
            nextT = t + nextDelay

            currentMeasures = [(x,[z for z in y if z < nextT]) for x,y in FullMeasures]
            for subMeasure in ((x,y) for x,y in currentMeasures if len(y) > 0):
                for key, val in (zip(zip(itertools.cycle(subMeasure[0]),subMeasure[1]), itertools.cycle([eval(subMeasure[0], namespace)]))):
                    Samples[key] += 1
                    Means[key] += (val - Means[key]) / float(Samples[key])
                    Variances[key] += float(Samples[key] - 1)/float(Samples[key]) * (val - Means[key])*(val - Means[key])
            FullMeasures = [(x[0],sorted(set(y[1]) ^ set(x[1]))) for x,y in zip(currentMeasures, FullMeasures)]

            for expr in Delta[nextEventIdx]:
                lval, rval = map(str.strip, expr.split('='))
                namespace[lval] = eval(rval, namespace)
                #exec(expr) in namespace

            enabled = [(eval(x, namespace) and (eval(y, namespace) > 0.0)) for x,y in zip(Phi, Lambda)]
            t = nextT

        currentMeasures = FullMeasures
        for subMeasure in ((x,y) for x,y in currentMeasures if len(y) > 0):
            for key, val in (zip(zip(itertools.cycle(subMeasure[0]),subMeasure[1]), itertools.cycle([eval(subMeasure[0], namespace)]))):
                Samples[key] += 1
                Means[key] += (val - Means[key]) / float(Samples[key])
                Variances[key] += float(Samples[key] - 1)/float(Samples[key]) * (val - Means[key])*(val - Means[key])

        FullMeasures = [(x,list(np.arange(y[0], y[1]+y[2], y[2]))) for x,y in Measures]

    for key in Variances.keys():
        if (Samples[key] > 0.0):
            Variances[key] /= Samples[key]
            ConfInterval[key] = 1.96 * math.sqrt(Variances[key])/math.sqrt(Samples[key])
        else:
            ConfInterval[key] = 0.0

    results = pd.DataFrame.from_dict({"Mean " + sv:{x[1]:y for x,y in Means.items() if x[0] == sv} for sv in SV})
    results = results.merge(pd.DataFrame.from_dict({"Variance " + sv:{x[1]:y for x,y in Variances.items() if x[0] == sv} for sv in SV}), left_index=True, right_index=True)
    results = results.merge(pd.DataFrame.from_dict({"Confidence Interval " + sv:{x[1]:y for x,y in ConfInterval.items() if x[0] == sv} for sv in SV}), left_index=True, right_index=True)
    return(results)
