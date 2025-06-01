(ns clojure.gdx.app)

(defprotocol App
  (post-runnable! [_ runnable]))
