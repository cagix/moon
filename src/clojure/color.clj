(ns clojure.color)

(def black [0 0 0 1])
(def white [1 1 1 1])
(def gray  [0.5 0.5 0.5 1])
(def red   [1 0 0 1])

; ported from com.badlogic.gdx.utils.NumberUtils/intToFloatColor
(defn- int-to-float-color
  "Encodes an ABGR int color as a float, masking alpha bits to avoid NaN range."
  [^long value]
  (Float/intBitsToFloat (unchecked-int (bit-and value 0xfeffffff))))

; ported from com.badlogic.gdx.graphics.Color/toFloatBits
(defn float-bits
  "Packs the RGBA color components (floats 0–1) into a single 32-bit float.
  The bits are arranged in ABGR order."
  [[r g b a]]
  (let [color (bit-or
               (bit-shift-left (int (* 255 (float a))) 24)
               (bit-shift-left (int (* 255 (float b))) 16)
               (bit-shift-left (int (* 255 (float g))) 8)
               (int (* 255 (float r))))]
    (int-to-float-color (unchecked-int color))))
