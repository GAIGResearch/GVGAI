/**
 * Code written by Adrien Couetoux, acouetoux@ulg.ac.be It is free to use,
 * distribute, and modify. User: adrienctx Date: 12/01/2015
 */
package adrienctx;

import java.util.Arrays;

class Transition {

    public final IntDoubleHashMap[] features1;

    private final IntDoubleHashMap[] features2;

    public final double reward;

    public final int action;

    public final boolean isFinal;

    public final IntArrayOfDoubleHashMap[] basisFunctionValues1;

    public final IntArrayOfDoubleHashMap[] basisFunctionValues2;

    public Transition(IntDoubleHashMap[] _f1, int _a, IntDoubleHashMap[] _f2, double _r, boolean _final, IntArrayOfDoubleHashMap[] _bf1, IntArrayOfDoubleHashMap[] _bf2) {
        features1 = _f1;
        features2 = _f2;
        reward = _r;
        action = _a;
        isFinal = _final;
        basisFunctionValues1 = _bf1;
        basisFunctionValues2 = _bf2;
    }

    public boolean equals(Transition _t) {
        if (!Arrays.equals(this.features1, _t.features1)) {
            return false;
        }
        if (!Arrays.equals(this.features2, _t.features2)) {
            return false;
        }
        if (!(this.reward == _t.reward)) {
            return false;
        }
        if (!(this.action == _t.action)) {
            return false;
        }
        return this.isFinal == _t.isFinal;

    }

    public void printTransition() {
        System.out.format("%n f1 : ");
        System.out.println(Arrays.toString(features1));
        System.out.format(" r : %f ", reward);
        System.out.format("%n f2 : ");
        System.out.println(Arrays.toString(features2));
    }
}
