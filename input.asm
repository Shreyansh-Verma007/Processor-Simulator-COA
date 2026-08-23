# ---- bubble sort initialization ----
    LI   x11, 11         # array size (n)

outer_loop:
    ADDI x11, x11, -1    # n--
    BLT  x11, x0, sorted # if n < 0, done

    LI   x12, 0          # j = 0

inner_loop:
    BGE  x12, x11, outer_loop # if j >= n, end inner loop

    # address calculation: addr = x10 + j * 4
    ADD  x13, x12, x12   # x13 = j * 2
    ADD  x13, x13, x13   # x13 = j * 4
    ADD  x13, x13, x10   # addr = offset + base
    
    LW   x14, 0(x13)     # a = arr[j]
    LW   x15, 4(x13)     # b = arr[j+1]

    BGE  x15, x14, no_swap # if b >= a, skip swap
    
    # swap
    SW   x15, 0(x13)
    SW   x14, 4(x13)

no_swap:
    ADDI x12, x12, 1     # j++
    JAL  x0, inner_loop

sorted:
    # Load sorted array into registers to verify in dump
    LW   x20, 0(x10)
    LW   x21, 4(x10)
    LW   x22, 8(x10)
    LW   x23, 12(x10)
    LW   x24, 16(x10)
    LW   x25, 20(x10)
    LW   x26, 24(x10)
    LW   x27, 28(x10)
    LW   x28, 32(x10)
    LW   x29, 36(x10)
    LW   x30, 40(x10)

    # NOPs to drain pipeline before dump
    ADDI x0, x0, 0
    ADDI x0, x0, 0
    ADDI x0, x0, 0

    ECALL
    HALT