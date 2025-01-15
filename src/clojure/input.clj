(ns clojure.input)

(defprotocol Input
  (x [_])
  (y [_])
  (button-just-pressed? [_ button])
  (key-just-pressed? [_ key])
  (key-pressed? [_ key])
  (set-processor [_ input-processor]))
