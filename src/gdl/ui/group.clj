(ns gdl.ui.group)

(defprotocol Group
  (find-actor [_ name])
  (clear-children! [_])
  (children [_]))
