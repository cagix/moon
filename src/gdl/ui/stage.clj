(ns gdl.ui.stage)

(defprotocol Stage
  (render! [_ ctx])
  (add! [_ actor-or-decl])
  (clear! [_])
  (hit [_ position])
  (find-actor [_ actor-name]))
