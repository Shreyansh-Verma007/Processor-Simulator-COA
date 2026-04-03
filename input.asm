# ============================================================================
#   Large .data demo - cache exercise
#   Initializes and sums two 256-word arrays (~2 KB) to stress L1 D-cache.
#   Pass 1: data_a[i] = i, data_b[i] = i
#   Pass 2: data_a[i] += data_b[i]
#   Pass 3: checksum of data_a into x8
# ============================================================================

.data
    data_a: .space 1024          # 256 words (4 bytes each)
    data_b: .space 1024          # 256 words
    count:  .word 256            # loop count constant

.text
    # -------- Pass 1: initialize arrays --------
    LI   x5, 256                 # loop counter
    LA   x4, data_a              # ptr A
    LA   x6, data_b              # ptr B
    ADDI x7, x0, 0               # i = 0

init_loop:
    SW   x7, 0(x4)               # A[i] = i
    SW   x7, 0(x6)               # B[i] = i
    ADDI x4, x4, 4
    ADDI x6, x6, 4
    ADDI x7, x7, 1
    ADDI x5, x5, -1
    BNE  x5, x0, init_loop

    # -------- Pass 2: vector add A[i] += B[i] --------
    LI   x5, 256
    LA   x4, data_a
    LA   x6, data_b

sum_loop:
    LW   x1, 0(x4)
    LW   x2, 0(x6)
    ADD  x3, x1, x2
    SW   x3, 0(x4)
    ADDI x4, x4, 4
    ADDI x6, x6, 4
    ADDI x5, x5, -1
    BNE  x5, x0, sum_loop

    # -------- Pass 3: checksum of A into x8 --------
    LI   x5, 256
    LA   x4, data_a
    ADDI x8, x0, 0

checksum_loop:
    LW   x1, 0(x4)
    ADD  x8, x8, x1
    ADDI x4, x4, 4
    ADDI x5, x5, -1
    BNE  x5, x0, checksum_loop

    ECALL
    HALT
