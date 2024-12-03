(ns ^:no-doc forge.app.screens
  (:require [forge.core :refer :all])
  (:import (com.badlogic.gdx Gdx)
           (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.utils ScreenUtils)))

(defrecord StageScreen [^Stage stage sub-screen]
  forge.core/Screen
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
    (.dispose stage)
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

(defmethods :app/screens
  (app-create [[_ {:keys [ks first-k]}]]
    (bind-root #'screens (mapvals stage-screen (mapvals
                                                (fn [ns-sym]
                                                  (require ns-sym)
                                                  ((ns-resolve ns-sym 'create)))
                                                ks)))
    (change-screen first-k))
  (app-dispose [_]
    (run! screen-destroy (vals screens)))
  (app-render [_]
    (ScreenUtils/clear forge.core/black)
    (screen-render (current-screen))))
