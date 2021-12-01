package com.sinux.pocketboard.utils;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Custom {@link LinkedList<E>} implementation with additional features:
 * - Automatically evicts last elements if it's size exceeds the limit
 * - Doesn't allow duplicates
 * - Moves added items to the beginning of list
 */
public class LruList<E> extends LinkedList<E> {

    private final int sizeLimit;

    public LruList(int sizeLimit) {
        this.sizeLimit = sizeLimit;
    }

    @Override
    public boolean add(E e) {
        super.remove(e);
        super.addFirst(e);
        evict();
        return true;
    }

    @Override
    public void addFirst(E e) {
        add(e);
    }

    @Override
    public void addLast(E e) {
        super.remove(e);
        super.addLast(e);
        evict();
    }

    @Override
    public boolean addAll(@NonNull Collection<? extends E> c) {
        super.removeAll(c);
        super.addAll(0, c);
        evict();
        return true;
    }

    private void evict() {
        while (size() > sizeLimit) {
            super.removeLast();
        }
    }
}
