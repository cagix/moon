(ns clojure.input)

(defprotocol Input
  (button-just-pressed? [_ button])
  (key-pressed? [_ key])
  (key-just-pressed? [_ key])
  (mouse-position [_])
  (set-processor! [_ input-processor]))
