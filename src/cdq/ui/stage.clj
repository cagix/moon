(ns cdq.ui.stage)

(defprotocol Stage
  (render! [_ ctx])
  (add! [_ actor-or-decl])
  (clear! [_])
  (root [_]))
