(ns gdl.application)

(defprotocol Application
  (post-runnable! [_ runnable]))
