package data;

/**
 * User: Cynric
 * Date: 15/5/24
 * Time: 16:07
 */
public class Register {
    public Integer registers[];
    public int sample;

    public Register() {
        registers = new Integer[32];
        for (int i = 0; i < 32; i++) {
            registers[i] = new Integer(0);
        }
    }
}
