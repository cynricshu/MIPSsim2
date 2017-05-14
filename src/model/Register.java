package model;

/**
 * User: yichli
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
