(ns clojure.gdx.scene2d.input-event
  (:import (com.badlogic.gdx.scenes.scene2d InputEvent)))

(defn stage [event]
  (InputEvent/.getStage event))
