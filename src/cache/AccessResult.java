package cache;

/**
 * Result of a cache/memory access: the data word and total latency in cycles.
 */
public class AccessResult {
    public final int data;
    public final int latencyCycles;

    public AccessResult(int data, int latencyCycles) {
        this.data = data;
        this.latencyCycles = latencyCycles;
    }
}
