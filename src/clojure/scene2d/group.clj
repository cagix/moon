(ns clojure.scene2d.group)

(defprotocol Group
  (add! [group actor-or-decl])
  (find-actor [group name])
  (clear-children! [group])
  (children [group]))
