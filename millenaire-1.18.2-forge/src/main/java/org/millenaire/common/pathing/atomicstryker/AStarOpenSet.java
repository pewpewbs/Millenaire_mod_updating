package org.millenaire.common.pathing.atomicstryker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Coordinate-indexed min heap. Costs may only be changed through improve(). */
final class AStarOpenSet {
    record Position(int x, int y, int z) {
        static Position of(AStarNode node) { return new Position(node.x, node.y, node.z); }
    }
    private final ArrayList<AStarNode> heap = new ArrayList<>();
    private final Map<Position, Integer> indices = new HashMap<>();
    boolean isEmpty() { return heap.isEmpty(); }
    AStarNode peek() { return heap.isEmpty() ? null : heap.get(0); }
    void offer(AStarNode node) {
        Position key = Position.of(node);
        if (indices.containsKey(key)) {
            improve(node, node.parent);
            return;
        }
        int index = heap.size();
        heap.add(node); indices.put(key, index); up(index);
    }
    boolean improve(AStarNode candidate, AStarNode parent) {
        Integer index = indices.get(Position.of(candidate));
        if (index == null) return false;
        if (heap.get(index).updateDistance(candidate.getG(), parent)) up(index);
        return true;
    }
    AStarNode poll() {
        if (heap.isEmpty()) return null;
        AStarNode result = heap.get(0);
        AStarNode last = heap.remove(heap.size() - 1);
        indices.remove(Position.of(result));
        if (!heap.isEmpty()) {
            heap.set(0, last); indices.put(Position.of(last), 0); down(0);
        }
        return result;
    }
    private void swap(int a, int b) {
        AStarNode first = heap.get(a), second = heap.get(b);
        heap.set(a, second); heap.set(b, first);
        indices.put(Position.of(second), a); indices.put(Position.of(first), b);
    }
    private void up(int index) {
        while (index > 0) {
            int parent = (index - 1) / 2;
            if (heap.get(parent).compareTo(heap.get(index)) <= 0) break;
            swap(parent, index); index = parent;
        }
    }
    private void down(int index) {
        while (index * 2 + 1 < heap.size()) {
            int child = index * 2 + 1;
            if (child + 1 < heap.size() && heap.get(child + 1).compareTo(heap.get(child)) < 0) child++;
            if (heap.get(index).compareTo(heap.get(child)) <= 0) break;
            swap(index, child); index = child;
        }
    }
}
