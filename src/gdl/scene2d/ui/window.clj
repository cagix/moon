(ns gdl.scene2d.ui.window)

(defprotocol TitleBar
  (title-bar? [actor] "Returns true if the actor is a window title bar."))
