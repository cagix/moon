(ns clojure.gdx.scene2d.event
  (:import (com.badlogic.gdx.scenes.scene2d Event)))

(defn stage [event]
  (Event/.getStage event))
