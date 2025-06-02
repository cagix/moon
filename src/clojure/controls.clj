(ns clojure.controls)

(defprotocol PlayerMovementInput
  (player-movement-vector [_]))
