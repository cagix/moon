(ns clojure.app)

(defprotocol App
  (post-runnable! [_ runnable]))
