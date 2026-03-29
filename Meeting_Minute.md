# Meeting Minutes
## Phase 1

---

### Date: 16th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Discussed which language to be used to build the simulator.
- Decided to use Java as it is a versatile programming language and highly suitable for developing a simulator, following the principles of OOP.

---

### Date: 17th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Initialized GitHub repo with a basic README.
- Started building the architecture to be followed in UML.
- Studied the related concepts regarding different pipelining stages at a hardware level.

---

### Date: 20th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Decided what all instructions should be supported by the simulator.
- Defined the U-Type instructions to be system instructions.

---

### Date: 22nd Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Decided how configurations are to be implemented (i.e cycles taken by each stage etc).
- Started building the lexer which is responsible for extracting lines of instructions from the input.

---

### Date: 24th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Built the parser which is responsible for parsing the instructions provided by the lexer.
- Built the CompilationResult and Compiler.

---

### Date: 25th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Implemented Instruction Fetch (IF) and Instruction Decode (ID) pipeline stages.
- Designed the core Register File architecture for instruction decoding.

---

### Date: 27th Feb 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Completed the Execution (EX), Memory (MEM), and Write Back (WB) stages.
- Connected the full 5-stage pipeline for sequential instruction execution.

---

### Date: 2nd Mar 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Designed the core package components (i.e Memory, Processer, RegisterFile)
- Enhanced simulation execution workflow to track cycles, stalls, and output statistics correctly to the console.

---

### Date: 4th Mar 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Integrated the Hazard Unit completely with the ID and EX stages to handle logic forwarding and pipeline flushing.

---

### Date: 8th Mar 2025
**Members:** Suhail Sahib, Shreyansh Verma

#### Decisions:
- Fixed bugs related to the Hazard Unit and pipeline flushing.