function main(β, γ, μ, n)
    @grounding begin
        S => Noun(person)
        I => Noun(patient)
        J => Noun(patient)
        R => Noun(person)
        λ₁ => Verb(h3n2)
        λ₄ => Verb(h1n1)
        λ₅ => Verb(treatment)
        λ₂ => Verb(treatment)
        λ₃ => Verb(loss_of_immunity)
    end

    @reaction begin
        λ₁, S + I -> 2I
        λ₄, S + J -> 2J
        λ₅, J -> R
        λ₂, I -> R
        λ₃, R -> S
    end, λ₁, λ₂, λ₃, λ₄, λ₅

    # β, 1S + 1I -> 0S + 2I
    # γ, 0R + 1I -> 0I + 1R
    # μ, 1R + 0S -> 1S + 0R

    Δ = [
        (S,I) -> (S-1, I+1),
        (S,J) -> (S-1, J+1),
        (J,R) -> (J-1, R+1),
        (I,R) -> (I-1, R+1),
        (R,S) -> (R-1, S+1),
    ]

    ϕ = [
        (S, I) -> S > 0 && I > 0,
        (S, J) -> S > 0 && J > 0,
        (J) -> J > 0,
        (I) -> I > 0,
        (R) -> R > 0,
    ]

    Λ = [
        λ₁(S,I) = begin β*S*I/n end,
        λ₄(S,J) = begin β*S*J/n end,
        λ₅(J) = begin γ*J end,
        λ₂(I) = begin γ*I end,
        λ₃(R) = begin μ*R end
    ]
    m = Petri.Model(g, Δ, ϕ, Λ)
    d = convert(ODEProblem, m)
    soln = solve(m) #discrete
    soln = solve(d) #continuos
end
