(ns cdq.effect)

(defprotocol Effect
  (applicable? [_ effect-ctx])
  (useful?     [_ effect-ctx world])
  (handle      [_ effect-ctx world])
  (draw        [_ effect-ctx ctx]))
