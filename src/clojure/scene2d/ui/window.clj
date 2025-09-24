(ns clojure.scene2d.ui.window)

(defprotocol TitleBar
  (title-bar? [actor] "Returns true if the actor is a window title bar."))

(defprotocol Ancestor
  (find-ancestor [actor]
                 "Finds the ancestor window of actor, otherwise throws an error if none of recursively searched parents of actors is a window actor."))
