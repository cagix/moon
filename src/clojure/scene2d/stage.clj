(ns clojure.scene2d.stage)

(defprotocol Stage
  (get-ctx [_])
  (set-ctx! [_ ctx])
  (act! [_])
  (draw! [_])
  (add! [_ actor])
  (clear! [_])
  (root [_])
  (hit [_ [x y]])
  (viewport [_]))
