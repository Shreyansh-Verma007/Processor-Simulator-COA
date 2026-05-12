# --------------------------------------------------------- #
# Template according to the Simulator:                      #
# --------------------------------------------------------- #
# Supported Sections:                                       #
#   .data   - For data initialization (memory variables)    #
#   .text   - For instruction logic                         #
#                                                           #
# Supported Data Directives (.data):                        #
#   .word, .half, .byte, .space, .zero, .ascii, .asciiz,    #
#   .string, .align, .globl, .global                        #
#                                                           #
# Supported Instructions (.text):                           #
#   R-Type: ADD, SUB, MUL, DIV, SLL, SRL, XOR, OR, AND      #
#   I-Type (ALU): ADDI                                      #
#   Pseudo: LI, LA, MV, NOP                                 #
#   Memory: LW, LB, SW, SB                                  #
#   Branch: BEQ, BNE, BLT, BGE                              #
#   Jumps:  JAL                                             #
#   System: ECALL, HALT                                     #
#                                                           #
# Registers: x0-x31 or general ABI names (zero, a0, t0..t6) #
# Comments: starts with # or //                             #
# --------------------------------------------------------- #

.data
arr: .word 9, 7, 5, 3, 1, 2, 4, 6, 8, 15, 14, 13, 12, 11, 10, 16, 17, 17, 18, 18, 18, 19, 20
n:   .word 23

.text
.globl main
main:
    LA x1, arr          # base address of array
    LA x2, n
    LW x2, 0(x2)        # x2 = n
    ADDI x3, x0, 0      # i = 0

outer:
    BGE x3, x2, end     # if i >= n → end

    ADDI x4, x0, 0      # j = 0
    SUB x5, x2, x3
    ADDI x5, x5, -1     # limit = n - i - 1

inner:
    BGE x4, x5, next    # if j >= limit → next outer

    LI x10, 2
    SLL x6, x4, x10     # j * 4
    ADD x7, x1, x6      # &arr[j]

    LW x8, 0(x7)        # arr[j]
    LW x9, 4(x7)        # arr[j+1]

    # if arr[j] <= arr[j+1] skip swap
    BGE x9, x8, skip

    SW x9, 0(x7)
    SW x8, 4(x7)

skip:
    ADDI x4, x4, 1
    BEQ x0, x0, inner   # unconditional jump

next:
    ADDI x3, x3, 1
    BEQ x0, x0, outer

end:
    HALT