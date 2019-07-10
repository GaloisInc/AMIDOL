using DifferentialEquations
using Plots

#= derivitive of the SIR system of ordinary differential equations
  Args:
      du (array): array of differential equations defining the change to the state variables
      u (array): array of the current state of the variables s, i, and r
      p (array): array of constants beta and gamma
      t (tuple): time domain

=#
function sir(du,u,p,t)
  s,i,r = u
  β, γ = p
  # Delta s
  du[1] = ds = -β*s*i/(s+i+r)
  # Delta i
  du[2] = di = -γ*i + β*s*i/(s+i+r)
  # Delta r
  du[3] = dr = γ*i
end

# float array: Initial values of s, i, and r
u0 = [500.0; 10.0; 0.0]
# float tuple: time domain of the problem
tspan = (0.0,100.0)
# float array: values of beta and gamma
p = [2.0 * 1.0/3.0, 1.0/3.0]
prob = ODEProblem(sir, u0, tspan, p)
sol = solve(prob)

plot(sol)
savefig("Julia-ODE.pdf")
