(ns clojure.application)

(defprotocol Application
  (post-runnable [_ runnable]))
