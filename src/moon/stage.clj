(ns moon.stage
  (:require [gdl.input :as input]
            [gdl.utils :refer [dispose]]
            [moon.graphics :as g]
            [moon.screen :as screen]
            [gdl.ui.stage :as stage]))

(defrecord StageScreen [stage sub-screen]
  screen/Screen
  (screen/enter! [_]
    (input/set-processor stage)
    (when sub-screen (screen/enter! sub-screen)))

  (screen/exit! [_]
    (input/set-processor nil)
    (when sub-screen (screen/exit! sub-screen)))

  (screen/render! [_]
    ; stage act first so sub-screen calls screen/change!
    ; -> is the end of frame
    ; otherwise would need render-after-stage
    ; or on screen/change! the stage of the current screen would still .act
    (stage/act! stage)
    (when sub-screen (screen/render! sub-screen))
    (stage/draw! stage))

  (screen/dispose! [_]
    (dispose stage)
    (when sub-screen (screen/dispose! sub-screen))))

(defn create
  "Actors or screen can be nil."
  [& {:keys [actors screen]}]
  (let [stage (stage/create (:viewport g/gui-view) g/batch)]
    (run! #(stage/add! stage %) actors)
    (map->StageScreen {:stage stage
                       :sub-screen screen})))

(defn stage-get []
  (:stage (screen/current)))

(defn mouse-on-actor? []
  (stage/hit (stage-get) (g/gui-mouse-position) :touchable? true))

(defn stage-add! [actor]
  (stage/add! (stage-get) actor))
