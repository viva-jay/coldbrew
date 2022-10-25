package v2;

import java.util.concurrent.atomic.AtomicInteger;

public class PortLoadBalancer {
    private AtomicInteger index = new AtomicInteger();
    private int[] ports;

    public PortLoadBalancer(int[] ports) {
        this.ports = ports;
    }
    public int getNext() {
        return ports[index.getAndIncrement() & ports.length - 1];
    }
}
