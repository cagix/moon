(ns forge.app.screens
  (:require [forge.core :refer [gui-viewport
                                batch
                                find-actor-with-id
                                bind-root
                                dispose
                                mapvals
                                Screen
                                screens
                                screen-enter
                                screen-exit
                                screen-render
                                screen-destroy
                                current-screen
                                change-screen]])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.utils ScreenUtils)))

(defrecord StageScreen [^Stage stage sub-screen]
  Screen
  (screen-enter [_]
    (.setInputProcessor Gdx/input stage)
    (screen-enter sub-screen))

  (screen-exit [_]
    (.setInputProcessor Gdx/input nil)
    (screen-exit sub-screen))

  (screen-render [_]
    (.act stage)
    (screen-render sub-screen)
    (.draw stage))

  (screen-destroy [_]
    (dispose stage)
    (screen-destroy sub-screen)))

(defn- stage-screen
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (proxy [Stage clojure.lang.ILookup] [gui-viewport batch]
                (valAt
                  ([id]
                   (find-actor-with-id (Stage/.getRoot this) id))
                  ([id not-found]
                   (or (find-actor-with-id (Stage/.getRoot this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage screen)))

(defn create [{:keys [ks first-k]}]
  (bind-root #'screens (mapvals stage-screen (mapvals
                                              (fn [ns-sym]
                                                (require ns-sym)
                                                ((ns-resolve ns-sym 'create)))
                                              ks)))
  (change-screen first-k))

(defn destroy []
  (run! screen-destroy (vals screens)))

(defn render []
  (ScreenUtils/clear Color/BLACK)
  (screen-render (current-screen)))
