(ns gdl.input) ; <- gdl is actually a purely protocol based game engine with one GDX implementation

(defprotocol Input
  (button-just-pressed? [_ button])
  (key-pressed? [_ key])
  (key-just-pressed? [_ key]))
