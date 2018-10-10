package NovelTS;
import java.util.Objects;
public class Atom {
    int data1;
    int data2;
    int data3;

    public Atom(int data1, int data2, int data3) {
        this.data1 = data1;
        this.data2 = data2;
        this.data3 = data3;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (o == this)
            return true;
        if (!(o instanceof Atom)) {
            return false;
        }
        return data1 == ((Atom) o).data1 && ((Atom) o).data2 == ((Atom) o).data2
                && data3 == ((Atom) o).data3;
    }

    @Override
    public int hashCode() {
        return this.data1 + this.data2 * 128 + this.data3 * 128 * 128;
    }
    @Override
    public String toString() {
        return data1 + " " + data2 + " " + data3 + " ";
    }

    public int getData1() {
        return data1;
    }

    public int getData2() {
        return data2;
    }

    public int getData3() {
        return data3;
    }
}
