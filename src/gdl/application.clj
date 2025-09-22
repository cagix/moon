(ns gdl.application)

(defprotocol Application
  (post-runnable! [_ f]))
