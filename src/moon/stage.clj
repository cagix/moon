(ns moon.stage
  (:refer-clojure :exclude [get])
  (:require [gdl.input :as input]
            [gdl.ui.stage :as stage]
            [gdl.utils :refer [dispose]]
            [moon.graphics.batch :refer [batch]]
            [moon.graphics.gui-view :as gui-view]
            [moon.screen :as screen]))

(defrecord StageScreen [stage sub-screen]
  screen/Screen
  (screen/enter [_]
    (input/set-processor stage)
    (when sub-screen (screen/enter sub-screen)))

  (screen/exit [_]
    (input/set-processor nil)
    (when sub-screen (screen/exit sub-screen)))

  (screen/render [_]
    ; stage act first so sub-screen calls screen/change
    ; -> is the end of frame
    ; otherwise would need render-after-stage
    ; or on screen/change the stage of the current screen would still .act
    (stage/act! stage)
    (when sub-screen (screen/render sub-screen))
    (stage/draw! stage))

  (screen/dispose [_]
    (dispose stage)
    (when sub-screen (screen/dispose sub-screen))))

(defn create
  "Actors or screen can be nil."
  [& {:keys [actors screen]}]
  (let [stage (stage/create (gui-view/viewport) batch)]
    (run! #(stage/add! stage %) actors)
    (map->StageScreen {:stage stage
                       :sub-screen screen})))

(defn get []
  (:stage (screen/current)))

(defn mouse-on-actor? []
  (stage/hit (get) (gui-view/mouse-position) :touchable? true))

(defn add! [actor]
  (stage/add! (get) actor))

(defn reset [new-actors]
  (let [stage (get)]
    (stage/clear! stage)
    (run! #(stage/add! stage %) new-actors)))
