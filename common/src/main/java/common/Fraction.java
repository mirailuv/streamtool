package common;

public class Fraction {
    public static int compare(Fraction a, Fraction b) {
        Long aX = 1L * a.a * b.b;
        Long bX = 1L * b.a * a.b;

        if (aX > bX) return 1; else if (aX < bX) return -1; else return 0;
    }

    int a = 0;
    int b = 1;

    public String get() {
        return a + "/" + b;
    }

    public String getPercent() {
        int i = (int) Math.round(a * 1000.0 / b);
        if (i < 0) i = 0;
        String s = i + "";
        if (s.length() == 1) s = "0" + s;

        String p = s.substring(0, s.length() - 1) + "." + s.substring(s.length() - 1, s.length()) + "%";

        return p;
    }

    public void simplify() {
        int maxInit = a;
        if (b > a) maxInit = b;

        int max = maxInit;

        for (int i = 0; i < MathHelp.primes.length; i++) {
            int c = MathHelp.primes[i];
            if (c > max) break;

            if (MathHelp.div(a, c) && MathHelp.div(b, c)) {
                a /= c;
                b /= c;
                i = -1;

                maxInit = a;
                if (b > a) maxInit = b;

                max = maxInit;
                continue;
            }

            max = maxInit / c;
        }
    }

    public Fraction(String source) {
        if (!source.contains("/")) {
            System.out.println("No /");
            return;
        }

        String sourceA = null;
        String sourceB = null;

        for (int i = 0; i < source.length(); i++) {
            if (source.substring(0, i).contains("/")) {
                sourceA = source.substring(0, i - 1);
                sourceB = source.substring(i, source.length());
                break;
            }
        }

        if (sourceA == null) {
            System.out.println("sourceA null");
            return;
        }

        if (sourceB == null) {
            System.out.println("sourceB null");
            return;
        }

        int pA = 0;
        int pB = 1;

        try {
            pA = Integer.parseInt(sourceA);
            pB = Integer.parseInt(sourceB);
        } catch (NumberFormatException e) {
            System.out.println("NumberFormatException");
            System.out.println(sourceA.length() + " " + sourceB.length());
            return;
        }

        a = pA;
        b = pB;

        simplify();
    }

    public void div(int i) {
        b *= i;
        simplify();
    }

    public void add(Fraction f) {
        int x = f.a * b;
        a *= f.b;
        a += x;
        b *= f.b;
        simplify();
    }
}

class MathHelp {
    static int[] primes = new int[] {
        2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71, 73, 79, 83, 89, 97, 
        101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179, 181, 191, 193, 197, 199, 
        211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 
        307, 311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 
        401, 409, 419, 421, 431, 433, 439, 443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 
        503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587, 593, 599, 
        601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 
        701, 709, 719, 727, 733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 
        809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877, 881, 883, 887, 
        907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997
    };

    static boolean div(int a, int b) {
        int test = a;
        test /= b;
        test *= b;
        if (test == a) return true;

        return false;
    }
}
