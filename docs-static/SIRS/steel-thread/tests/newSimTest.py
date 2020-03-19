import numpy as np
import simpy
from collections import Counter
from tqdm import tqdm
import random
from math import log
import time

processList = list()

def Exponential(rate):
    return(random.expovariate(rate))

class AMIDOLRateReward():
    def __init__(self):
        self.rewards = dict()

    def accumulateReward(self, params):
        return(0.0)

    def getDelay(self, params):
        return(np.Infinity)

    def getSample(self, params):
        return(np.Infinity)

    def isEnabled(self, params):
        return(True)


class AMIDOLEvent():
    def getName(self):
        return("GenericEvent")

    def getRate(self, params):
        return(1.0)

    def getDelay(self, params):
        return(Exponential(self.getRate(params)))

    def isEnabled(self, params):
        return(True)

    def fireEvent(self, params):
        return()

def dummyFire(self, params):
    return()

class AMIDOLSim():
    def __init__(self, until=10.0):
        self.clock = 0.0
        self.until = until
        self.eventList = []
        self.rateRewardList = []
        self.processed = 0.0

    def registerEvent(self, event):
        self.eventList.append(event)

    def registerRateReward(self, reward):
        self.rateRewardList.append(reward)

    def stepSim(self, params):
        self.processed += 1.0
        nextEventTime = np.Infinity
        nextEventFire = dummyFire
        nextRewardTime = np.Infinity
        nextRewardAccum = dummyFire
        enabledEvents = []
        eventRates = []
        totalRates = 0.0

        for event in self.eventList:
            if (event.isEnabled(params)):
                enabledEvents.append(event)
                theRate = event.getRate(params)
                eventRates.append(theRate)
                totalRates += theRate

        chooseEvent = random.random()
        eventSum = 0.0
        for idx in range(0, len(eventRates)):
            eventSum += eventRates[idx]/totalRates
            if (eventSum >= chooseEvent):
                nextEventFire = enabledEvents[idx].fireEvent
                nextEventTime = self.clock + random.expovariate(totalRates)
                break

        for reward in self.rateRewardList:
            sample = reward.getSample(params)
            if ((sample) < nextRewardTime):
                nextRewardTime = sample
                nextRewardAccum = reward.accumulateReward
                break

        if ((nextEventTime is np.Infinity) and (nextRewardTime is np.Infinity)):
            self.clock = np.Infinity
            return()
        if (nextEventTime < nextRewardTime):
            self.clock = nextEventTime
            nextEventFire(params)
        else:
            self.clock = nextRewardTime
            nextRewardAccum(self.clock, params)

    def runSim(self, params):
        self.clock = 0.0
        while((not self.eventList == []) and (not self.rateRewardList == []) and (self.clock < self.until)):
            self.stepSim(params)

class AMIDOLParameters():
    def __init__(self):
        self.S_Pcinfect_S = 51999999
        self.ScinfectcI_Scinfect_IcI_Pccure_I = 1
        self.ScinfectcIccure_RcR_P = 0
        self.beta = 1.0/3.0*1.24
        self.gamma = 1.0/3.0

class infectEvent(AMIDOLEvent):
    def getName(self):
        return("InfectEvent")

    def getRate(self, v):
        return((v.beta*v.S_Pcinfect_S*v.ScinfectcI_Scinfect_IcI_Pccure_I/(v.S_Pcinfect_S+v.ScinfectcI_Scinfect_IcI_Pccure_I+v.ScinfectcIccure_RcR_P)))

    def isEnabled(self, v):
        return((v.beta*v.S_Pcinfect_S * v.ScinfectcI_Scinfect_IcI_Pccure_I) > 0.0)

    def fireEvent(self, v):
        v.S_Pcinfect_S -= 1.0
        v.ScinfectcI_Scinfect_IcI_Pccure_I += 1.0

class cureEvent(AMIDOLEvent):

    def getName(self):
        return("CureEvent")

    def getRate(self, v):
        return((v.gamma*v.ScinfectcI_Scinfect_IcI_Pccure_I))

    def isEnabled(self, v):
        return(v.gamma*v.ScinfectcI_Scinfect_IcI_Pccure_I > 0.0)

    def fireEvent(self, v):
        v.ScinfectcI_Scinfect_IcI_Pccure_I -= 1.0
        v.ScinfectcIccure_RcR_P += 1.0

class rvIRateReward(AMIDOLRateReward):
    def __init__(self):
        self.rewards = dict()
        self.samplePoints = list(np.arange(0.0, 100.0, 5.0))
        self.delays = list()
        self.delays.append(self.samplePoints[0])
        idx = 1
        lastX = self.samplePoints[0]
        for x in self.samplePoints[1:]:
            self.delays.append(x - lastX)
            lastX = x

    def accumulateReward(self, now, params):
        self.delays.pop(0)
        self.samplePoints.pop(0)
        self.rewards[now] = params.ScinfectcI_Scinfect_IcI_Pccure_I


    def getDelay(self, params):
        if (self.delays):
            return(self.delays[0])
        else:
            return(np.Infinity)

    def getSample(self, params):
        if (self.samplePoints):
            return(self.samplePoints[0])
        else:
            return(np.Infinity)

maxRuns = 100

rvICounter = dict()
processed = 0.0

perf =  time.perf_counter()
for trace in range(0, maxRuns):
    params = AMIDOLParameters()
    cure = cureEvent()
    infect = infectEvent()
    rvI = rvIRateReward()

    sim = AMIDOLSim(until=max(rvI.samplePoints))
    sim.registerEvent(cure)
    sim.registerEvent(infect)
    sim.registerRateReward(rvI)
    sim.runSim(params)

#    if not(rvICounter):
#        for key in rvI.rewards:
#            rvICounter[key] = rvI.rewards[key] / float(maxRuns)
#    else:
#        for key in rvI.rewards:
#            rvICounter[key] += rvI.rewards[key] / float(maxRuns)

    processed += sim.processed

#print(sorted(rvICounter.items()))
print(processed/(time.perf_counter() - perf))
