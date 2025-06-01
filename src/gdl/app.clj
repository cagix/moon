(ns gdl.app)

(defprotocol App
  (post-runnable! [_ runnable]))
