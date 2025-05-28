(ns gdl.input) ; <- gdl is actually a purely protocol based game engine with one GDX implementation
; => this is very very simple, same like sound or viewport
; but actully fucking amazing
; => thats why no global VISUI state also

(defprotocol Input
  (button-just-pressed? [_ button])
  (key-pressed? [_ key])
  (key-just-pressed? [_ key]))
