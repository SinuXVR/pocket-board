package com.sinux.pocketboard.utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class LruListTest {

    @Test
    public void lruListMaxCapacityTest() {
        LruList<Integer> lruList = new LruList<>(3);

        lruList.add(1);
        Assert.assertFalse(lruList.isEmpty());

        lruList.addAll(Arrays.asList(2, 3));
        Assert.assertEquals(3, lruList.size());

        lruList.add(4);
        Assert.assertEquals(3, lruList.size());

        lruList.addAll(Arrays.asList(5, 6));
        Assert.assertEquals(3, lruList.size());
    }

    @Test
    public void lruListConsistencyTest() {
        LruList<Integer> lruList = new LruList<>(3);

        lruList.addAll(Arrays.asList(1, 2, 3));
        Assert.assertTrue(lruList.containsAll(Arrays.asList(1, 2, 3)));

        lruList.addAll(Arrays.asList(4, 5));
        Assert.assertTrue(lruList.containsAll(Arrays.asList(4, 5, 1)));

        lruList.add(1);
        Assert.assertEquals(1, (int) lruList.get(0));
    }
}
