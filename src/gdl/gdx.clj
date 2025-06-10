(ns gdl.gdx)

(defprotocol Application
  (post-runnable! [_ runnable]))
