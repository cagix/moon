(ns forge.stage
  (:refer-clojure :exclude [get])
  (:require [forge.app :as app]
            [forge.graphics :refer [gui-mouse-position gui-viewport batch]]
            [forge.input :as input]
            [forge.ui :as ui])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)))

(defn get []
  (:stage (app/current-screen)))

(defrecord StageScreen [stage sub-screen]
  app/Screen
  (enter [_]
    (input/set-processor stage)
    (app/enter sub-screen))

  (exit [_]
    (input/set-processor nil)
    (app/exit sub-screen))

  (render [_]
    (.act stage)
    (app/render sub-screen)
    (.draw stage))

  (dispose [_]
    (.dispose stage)
    (app/dispose sub-screen)))

(defn- stage-create [viewport batch]
  (proxy [Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (ui/find-actor-with-id (.getRoot this) id))
      ([id not-found]
       (or (ui/find-actor-with-id (.getRoot this) id)
           not-found)))))

(defn create
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (stage-create gui-viewport batch)]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage screen)))

(defn mouse-on-actor? []
  (let [[x y] (gui-mouse-position)]
    (.hit (get) x y true)))

(defn add-actor [actor]
  (.addActor (get) actor))

(defn reset [new-actors]
  (.clear (get))
  (run! add-actor new-actors))
