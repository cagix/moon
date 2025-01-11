(ns clojure.app)

(defprotocol Application
  (post-runnable [_ runnable]))
