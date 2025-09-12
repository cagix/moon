(ns clojure.gdx.scenes.scene2d.input-event
  (:import (com.badlogic.gdx.scenes.scene2d InputEvent)))

(defn stage [event]
  (InputEvent/.getStage event))
