(ns gdl.scene2d.group)

(defprotocol Group
  (add! [group actor])
  (find-actor [group name])
  (clear-children! [group])
  (children [group])
  (set-opts! [_ opts]))
