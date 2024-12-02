(ns forge.stage
  (:require [forge.screen :as screen]
            [forge.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defrecord StageScreen [^Stage stage sub-screen]
  screen/Screen
  (enter [_]
    (set-input-processor stage)
    (screen/enter sub-screen))

  (exit [_]
    (set-input-processor nil)
    (screen/exit sub-screen))

  (render [_]
    (.act stage)
    (screen/render sub-screen)
    (.draw stage))

  (destroy [_]
    (dispose stage)
    (screen/destroy sub-screen)))

(defn- stage-create ^Stage [viewport batch]
  (proxy [Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (ui/find-actor-with-id (Stage/.getRoot this) id))
      ([id not-found]
       (or (ui/find-actor-with-id (Stage/.getRoot this) id)
           not-found)))))

(defn create
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (stage-create gui-viewport batch)]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage screen)))

(defn mouse-on-actor? []
  (let [[x y] (gui-mouse-position)]
    (.hit (screen-stage) x y true)))
