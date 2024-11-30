(ns forge.stage
  (:refer-clojure :exclude [get])
  (:require [forge.graphics :refer [gui-mouse-position gui-viewport batch]]
            [forge.screen :as screen]
            [forge.ui :as ui])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.scenes.scene2d Stage)))

(defn get ^Stage []
  (:stage (current-screen)))

(defrecord StageScreen [^Stage stage sub-screen]
  screen/Screen
  (enter [_]
    (.setInputProcessor Gdx/input stage)
    (screen/enter sub-screen))

  (exit [_]
    (.setInputProcessor Gdx/input nil)
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
    (.hit (get) x y true)))

(defn add-actor [actor]
  (.addActor (get) actor))

(defn reset [new-actors]
  (.clear (get))
  (run! add-actor new-actors))
