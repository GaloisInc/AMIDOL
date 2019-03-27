# Steel Thread for Crystalization/Stoichiometry model

We assume the user wants to build a model supporting chemical reactions restricted to the forms:

```
A --> B
A + C --> B
A --> B + D
2A --> B
A --> 2B
2A --> 2B
A + C --> 2B
2A --> B + D
```

To test this, we will build a simple crystalization problem based on the following:

https://minds.wisconsin.edu/bitstream/handle/1793/10884/file_1.pdf?sequence=1&origin=publication_detail

* The user creates a `species` noun and names it "A".  They set the variable "S" equal to 1000000.
* The user creates a `species` noun and names it "B".  They set the variable "S" equal to 0.
* The user creates a `species` noun and names it "C".  They set the variable "S" equal to 10.
* The user creates a `species` noun and names it "D".  They set the variable "S" equal to 0.
* The user creates a `reaction_1x2_1` verb and names it "k1".  They set the variable "k" to "0.00000005"
* The user creates a `reaction_2_1` verb and names it "k2".  They set the variable "k" to "0.0000001"
* The user connects "A" to "k1".
* The user connects "k1" to "B".
* The user connects "A" to "k2".
* The user connects "C" to "k2".
* The user connects "k2" to "D".