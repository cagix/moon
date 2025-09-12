(ns clojure.gdx.utils.number)

#_(defn int->float-color
  "Encodes the ABGR int color as a float. The alpha is compressed to use only even
  numbers between 0-254 to avoid using bits in the NaN range (see
  java.lang.Float/intBitsToFloat javadocs). Rendering which uses colors encoded
  as floats should expand the 0-254 back to 0-255, else colors cannot be fully opaque."
  [value]
  (Float/intBitsToFloat (bit-and (unchecked-int value) 0xfeffffff)))
;                                                 java.lang.Math.toIntExact                        Math.java: 1135
; java.lang.ArithmeticException: integer overflow
