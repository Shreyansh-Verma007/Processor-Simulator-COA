# Bubble Sort
.data
arr: .word 5, 3, 8, 1, 4

.text
LI x10, 5
LA x9, arr
ADDI x7, x10, -1
outer:
LI x6, 0
inner:
BGE x6, x7, next_outer
ADD x5, x6, x6
ADD x5, x5, x5
ADD x4, x9, x5
ADDI x3, x4, 4
LW x1, 0(x4)
LW x2, 0(x3)
BLT x1, x2, no_swap
SW x2, 0(x4)
SW x1, 0(x3)
no_swap:
ADDI x6, x6, 1
BLT x6, x7, inner
next_outer:
ADDI x7, x7, -1
BLT x0, x7, outer
ECALL
HALT
