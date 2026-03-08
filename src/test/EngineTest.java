import com.calculator.CalculatorEngine;

/** Quick smoke-test for the expression engine (no JUnit required). */
public class EngineTest {
    public static void main(String[] args) throws Exception {
        CalculatorEngine e = new CalculatorEngine();
        int pass = 0, fail = 0;

        // { expression, expected-result }
        Object[][] tests = {
            { "2+3",              5.0          },
            { "10/4",             2.5          },
            { "3*7",              21.0         },
            { "10-3",             7.0          },
            { "2^10",             1024.0       },
            { "2^3^2",            512.0        }, // right-associative: 2^(3^2)=2^9=512
            { "sqrt(144)",        12.0         },
            { "cbrt(27)",         3.0          },
            { "5!",               120.0        },
            { "sin(90)",          1.0          }, // degrees mode
            { "cos(0)",           1.0          },
            { "log(100)",         2.0          },
            { "ln(e)",            1.0          },
            { "log2(8)",          3.0          },
            { "pi",               Math.PI      },
            { "e",                Math.E       },
            { "abs(-7.5)",        7.5          },
            { "nthrt(3,27)",      3.0          }, // cube root of 27
            { "comb(5,2)",        10.0         },
            { "perm(5,2)",        20.0         },
            { "gcd(48,18)",       6.0          },
            { "lcm(4,6)",         12.0         },
            { "-(2+3)",           -5.0         },
            { "2*(3+4)",          14.0         },
            { "10%3",             1.0          },
            { "1.5e2",            150.0        }, // scientific notation
            { "floor(3.7)",       3.0          },
            { "ceil(3.2)",        4.0          },
            { "round(2.5)",       3.0          },
            { "max(3,7)",         7.0          },
            { "min(3,7)",         3.0          },
        };

        for (Object[] t : tests) {
            String expr     = (String) t[0];
            double expected = (double) t[1];
            try {
                double result = e.evaluate(expr);
                boolean ok = Math.abs(result - expected) < 1e-9
                          || (Math.abs(expected) > 1e-9 && Math.abs((result-expected)/expected) < 1e-9);
                if (ok) {
                    System.out.printf("PASS  %-20s = %s%n", expr, CalculatorEngine.formatResult(result));
                    pass++;
                } else {
                    System.out.printf("FAIL  %-20s expected=%.10g  got=%.10g%n", expr, expected, result);
                    fail++;
                }
            } catch (Exception ex) {
                System.out.printf("ERR   %-20s  (%s)%n", expr, ex.getMessage());
                fail++;
            }
        }

        System.out.printf("%n%d passed, %d failed%n", pass, fail);
        System.exit(fail > 0 ? 1 : 0);
    }
}
