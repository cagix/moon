(ns gdl.scene2d.group)

(defprotocol Group
  (add! [_ actor])
  (find-actor [_ name])
  (clear-children! [_])
  (children [_]))
