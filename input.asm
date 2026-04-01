# ════════════════════════════════════════════════════════════════════════════
#   .data segment demo — array sum
#   Sums data_a[i] + data_b[i] and stores to data_a[i]
#   Array length: 4 elements of 4 bytes each
# ════════════════════════════════════════════════════════════════════════════

.data
    data_a: .word 10, 20, 30, 40       # base array
    data_b: .word  1,  2,  3,  4       # values to add
    count:  .word 4                    # loop count

.text
    la   x4, count          # x4 = address of count
    LW   x5, 0(x4)          # x5 = 4 (loop count)
    la   x4, data_a         # x4 = base of array A
    la   x6, data_b         # x6 = base of array B

loop:
    LW   x1, 0(x4)          # x1 = A[i]
    LW   x2, 0(x6)          # x2 = B[i]
    ADD  x3, x1, x2         # x3 = A[i] + B[i]
    SW   x3, 0(x4)          # store back to A[i]
    ADDI x4, x4, 4          # advance A pointer
    ADDI x6, x6, 4          # advance B pointer
    ADDI x5, x5, -1         # decrement count
    BNE  x5, x0, loop       # if count != 0, repeat

    ADDI x0, x0, 0
    ADDI x0, x0, 0
    ADDI x0, x0, 0
    ECALL
    HALT