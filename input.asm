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
    # Array of numbers to sort
    array:  .word 29, 10, 14, 37, 13, 9, 5, 8, 2, 20
    size:   .word 10

.text
.globl main
main:
    LA a0, array         # a0 = base address of the array
    LA t0, size          # load address of size parameter
    LW a1, 0(t0)         # a1 = size of array (n)

    LI t2, 2
    BLT a1, t2, end_sort # If n < 2, the array is already sorted, so exit

outer_loop:
    LI t0, 0             # t0 = swapped flag (0 = false, 1 = true)
    LI t1, 0             # t1 = loop index (i = 0)
    ADDI t2, a1, -1      # t2 = n - 1 (inner loop limit)

inner_loop:
    BGE t1, t2, end_inner # if (i >= limit) exit inner loop

    # Calculate array[i] address: base_addr + (i * 4)
    # Simulator only supports R-type SLL (no SLLI)
    LI t3, 2
    SLL t4, t1, t3       # t4 = i * 4 
    ADD t4, a0, t4       # t4 = address of array[i]

    # Load elements
    LW t5, 0(t4)         # t5 = array[i]
    LW t6, 4(t4)         # t6 = array[i+1]

    # If array[i] <= array[i+1], skip swap
    # (Since there is no BLE instruction, we use BGE with reversed operands: array[i+1] >= array[i])
    BGE t6, t5, skip_swap

    # Swap elements in memory
    SW t6, 0(t4)         # array[i] = array[i+1]
    SW t5, 4(t4)         # array[i+1] = array[i]

    LI t0, 1             # swapped = true

skip_swap:
    ADDI t1, t1, 1       # i++
    BEQ zero, zero, inner_loop  # Unconditional branch to inner loop

end_inner:
    # If not swapped during this pass, the array is entirely sorted
    BEQ t0, zero, end_sort 
    
    # Optimization: the last element is correctly placed, so we lower the sort limit (n--)
    ADDI a1, a1, -1
    LI t3, 2
    BGE a1, t3, outer_loop # if limit >= 2, do another pass

end_sort:
    HALT                 # Terminate the simulator