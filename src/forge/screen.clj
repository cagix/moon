(ns forge.screen
  (:require [gdl.ui.stage :as stage]
            [gdl.utils :refer [dispose]])
  (:import (com.badlogic.gdx Gdx)))

(defprotocol Screen
  (enter   [_])
  (exit    [_])
  (render  [_])
  (dispose [_]))

(defrecord StageScreen [stage sub-screen]
  Screen
  (enter [_]
    (.setInputProcessor Gdx/input stage)
    (when sub-screen (enter sub-screen)))

  (exit [_]
    (.setInputProcessor Gdx/input nil)
    (when sub-screen (exit sub-screen)))

  (render [_]
    ; stage act first so sub-screen calls change
    ; -> is the end of frame
    ; otherwise would need render-after-stage
    ; or on change the stage of the current screen would still .act
    (stage/act! stage)
    (when sub-screen (render sub-screen))
    (stage/draw! stage))

  (dispose [_]
    (dispose stage)
    (when sub-screen (dispose sub-screen))))

(defn stage-screen
  "Actors or screen can be nil."
  [viewport batch {:keys [actors screen]}]
  (let [stage (stage/create viewport batch)]
    (run! #(stage/add! stage %) actors)
    (->StageScreen stage screen)))
