(ns app.lifecycle
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils :refer [clear-screen]]
            [forge.app :as app]
            [forge.assets :as assets]
            [forge.graphics :as graphics]
            [forge.db :as db]
            [forge.graphics.cursors :as cursors]
            [forge.ui :as ui]
            [forge.utils :refer [mapvals]]))

(defrecord StageScreen [stage sub-screen]
  app/Screen
  (enter [_]
    (gdx/set-input-processor stage)
    (app/enter sub-screen))

  (exit [_]
    (gdx/set-input-processor nil)
    (app/exit sub-screen))

  (render [_]
    (stage/act stage)
    (app/render sub-screen)
    (stage/draw stage))

  (dispose [_]
    (gdx/dispose stage)
    (app/dispose sub-screen)))

(defn- stage-create [viewport batch]
  (proxy [com.badlogic.gdx.scenes.scene2d.Stage clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (ui/find-actor-with-id (.getRoot this) id))
      ([id not-found]
       (or (ui/find-actor-with-id (.getRoot this) id)
           not-found)))))

(defn- stage-screen
  "Actors or screen can be nil."
  [{:keys [actors screen]}]
  (let [stage (stage-create graphics/gui-viewport graphics/batch)]
    (run! #(stage/add stage %) actors)
    (->StageScreen stage screen)))

(defn create [first-screen screens]
  (assets/init)
  (cursors/init)
  (graphics/init)
  (ui/load! :skin-scale/x1)
  (.bindRoot #'app/screens (mapvals stage-screen (screens)))
  (app/change-screen first-screen))

(defn dispose []
  (assets/dispose)
  (cursors/dispose)
  (graphics/dispose)
  (run! app/dispose (vals app/screens))
  (ui/dispose!))

(defn render []
  (clear-screen color/black)
  (app/render (app/current-screen)))

(defn resize [w h]
  (.update graphics/gui-viewport   w h true)
  (.update graphics/world-viewport w h))
