(ns gdl.ui.group)

(defprotocol Group
  (add! [_ actor-or-decl])
  (find-actor [_ name])
  (clear-children! [_])
  (children [_]))
