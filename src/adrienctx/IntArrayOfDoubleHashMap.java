/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adrienctx;

import java.util.HashMap;
import java.util.Set;

/**
 * @author acouetoux
 */
class IntArrayOfDoubleHashMap {
    private final HashMap<Integer, double[]> hashMap;

    public IntArrayOfDoubleHashMap() {
        hashMap = new HashMap<>();
    }

    public IntArrayOfDoubleHashMap(IntArrayOfDoubleHashMap original) {
        this.hashMap = new HashMap<>();
        for (Integer key : original.keySet()) {
            double[] doubleVector = new double[original.get(key).length];
            for (int i = 0; i < doubleVector.length; i++) {
                doubleVector[i] = original.get(key)[i];
            }
            this.hashMap.put(key, doubleVector);
        }
    }

    public double[] get(Integer key) {
        return hashMap.get(key);
    }

    public Set<Integer> keySet() {
        return hashMap.keySet();
    }

    public void put(Integer key, double[] value) {
        hashMap.put(key, value);
    }

    public boolean containsKey(Integer key) {
        return hashMap.containsKey(key);
    }

}
