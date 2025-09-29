(ns gdl.scene2d.stage)

(defprotocol Stage
  (set-ctx! [_ ctx])
  (get-ctx [_])
  (act! [_])
  (draw! [_])
  (add! [_ actor])
  (clear! [_])
  (root [_])
  (hit [_ [x y]])
  (viewport [_]))
