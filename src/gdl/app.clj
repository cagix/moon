(ns gdl.app)

(defprotocol Application
  (post-runnable [_ runnable]))
