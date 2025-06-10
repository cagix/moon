(ns gdl.input)

(defprotocol Input
  (set-input-processor! [_ input-processor])
  (button-just-pressed? [_ button])
  (key-pressed? [_ key])
  (key-just-pressed? [_ key])
  (mouse-position [_]))
