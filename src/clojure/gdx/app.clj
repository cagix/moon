(ns clojure.gdx.app)

(defprotocol Application
  (post-runnable! [_ runnable]))
