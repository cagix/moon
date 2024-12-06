(ns forge.app.screens
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.input :as input]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [forge.core :refer [gui-viewport
                                batch
                                find-actor-with-id
                                mapvals
                                Screen
                                screens
                                screen-enter
                                screen-exit
                                screen-render
                                screen-destroy
                                current-screen
                                change-screen]]
            [forge.utils :refer [bind-root]]))

(defrecord StageScreen [stage sub-screen]
  Screen
  (screen-enter [_]
    (input/set-processor stage)
    (screen-enter sub-screen))

  (screen-exit [_]
    (input/set-processor nil)
    (screen-exit sub-screen))

  (screen-render [_]
    (stage/act stage)
    (screen-render sub-screen)
    (stage/draw stage))

  (screen-destroy [_]
    (dispose stage)
    (screen-destroy sub-screen)))

(defn- stage-screen
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (proxy [com.badlogic.gdx.scenes.scene2d.Stage clojure.lang.ILookup] [gui-viewport batch]
                (valAt
                  ([id]
                   (find-actor-with-id (stage/root this) id))
                  ([id not-found]
                   (or (find-actor-with-id (stage/root this) id)
                       not-found))))]
    (run! #(.addActor stage %) actors)
    (->StageScreen stage screen)))

(defn create [[_ {:keys [ks first-k]}]]
  (bind-root screens (mapvals stage-screen (mapvals
                                            (fn [ns-sym]
                                              (require ns-sym)
                                              ((ns-resolve ns-sym 'create)))
                                            ks)))
  (change-screen first-k))

(defn destroy [_]
  (run! screen-destroy (vals screens)))

(defn render [_]
  (g/clear-screen color/black)
  (screen-render (current-screen)))
