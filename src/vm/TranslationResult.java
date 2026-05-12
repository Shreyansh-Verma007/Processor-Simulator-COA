package vm;

/**
 * Result of a virtual-to-physical address translation.
 * Contains the physical address and the total latency incurred
 * (TLB hit/miss + optional page walk + optional page fault).
 */
public class TranslationResult {
    public final int physicalAddress;
    public final int latencyCycles;

    public TranslationResult(int physicalAddress, int latencyCycles) {
        this.physicalAddress = physicalAddress;
        this.latencyCycles = latencyCycles;
    }
}
