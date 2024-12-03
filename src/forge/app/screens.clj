(ns ^:no-doc forge.app.screens
  (:require [clojure.gdx :as gdx]
            [forge.core :refer [screen-enter screen-exit screen-render screen-destroy find-actor-with-id
                                change-screen current-screen]]
            [forge.system :as system :refer [bind-root mapvals defmethods]])
  (:import (com.badlogic.gdx.scenes.scene2d Stage)
           (com.badlogic.gdx.utils ScreenUtils)))

(defrecord StageScreen [^Stage stage sub-screen]
  forge.core/Screen
  (screen-enter [_]
    (gdx/set-input-processor stage)
    (screen-enter sub-screen))

  (screen-exit [_]
    (gdx/set-input-processor nil)
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
  (let [stage (proxy [Stage clojure.lang.ILookup] [system/gui-viewport system/batch]
                (valAt
                  ([id]
                   (find-actor-with-id (Stage/.getRoot this) id))
                  ([id not-found]
                   (or (find-actor-with-id (Stage/.getRoot this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage screen)))

(defmethods :app/screens
  (system/create [[_ {:keys [ks first-k]}]]
    (bind-root #'system/screens (mapvals stage-screen (mapvals
                                                       (fn [ns-sym]
                                                         (require ns-sym)
                                                         ((ns-resolve ns-sym 'create)))
                                                       ks)))
    (change-screen first-k))
  (system/dispose [_]
    (run! screen-destroy (vals system/screens)))
  (system/render [_]
    (ScreenUtils/clear forge.core/black)
    (screen-render (current-screen))))
