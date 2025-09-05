(ns cdq.ui.stage)

(defprotocol Stage
  (render! [_ ctx])
  (add! [_ actor-or-decl])
  (clear! [_])
  (root [_])
  (hit [_ [x y]]))
