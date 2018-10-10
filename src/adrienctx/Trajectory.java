/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package adrienctx;

/**
 * @author acouetoux
 */
class Trajectory {

//    private final double reward;

    public final boolean isFinal;

//    private final int length;

    public final IntArrayOfDoubleHashMap[] basisFunctionValues1;

    public final IntArrayOfDoubleHashMap[] basisFunctionValues2;

    public Trajectory(boolean _final, IntArrayOfDoubleHashMap[] _bf1, IntArrayOfDoubleHashMap[] _bf2) {
//        IntDoubleHashMap[] features1 = _f1;
//        IntDoubleHashMap[] features2 = _f2;
//        reward = _r;
        isFinal = _final;
//        length = _length;
        basisFunctionValues1 = _bf1;
        basisFunctionValues2 = _bf2;
    }
}
