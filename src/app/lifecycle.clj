(ns app.lifecycle
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.scene2d.stage :as stage]
            [clojure.gdx.utils :as utils :refer [clear-screen]]
            [forge.app :as app]
            [forge.assets :as assets]
            [forge.graphics :as graphics]
            [forge.db :as db]
            [forge.graphics.cursors :as cursors]
            [app.screens :as screens]
            [forge.ui :as ui])
  (:import (com.badlogic.gdx.graphics Pixmap)))

(def ^:private props
  {:cursors/bag                   ["bag001"       [0   0]]
   :cursors/black-x               ["black_x"      [0   0]]
   :cursors/default               ["default"      [0   0]]
   :cursors/denied                ["denied"       [16 16]]
   :cursors/hand-before-grab      ["hand004"      [4  16]]
   :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
   :cursors/hand-grab             ["hand003"      [4  16]]
   :cursors/move-window           ["move002"      [16 16]]
   :cursors/no-skill-selected     ["denied003"    [0   0]]
   :cursors/over-button           ["hand002"      [0   0]]
   :cursors/sandclock             ["sandclock"    [16 16]]
   :cursors/skill-not-usable      ["x007"         [0   0]]
   :cursors/use-skill             ["pointer004"   [0   0]]
   :cursors/walking               ["walking"      [16 16]]})

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
    (utils/dispose stage)
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

(defn create []
  (assets/init)
  (bind-root #'cursors/cursors (mapvals (fn [[file hotspot]]
                                          (let [pixmap (Pixmap. (gdx/internal-file (str "cursors/" file ".png")))
                                                cursor (gdx/new-cursor pixmap hotspot)]
                                            (.dispose pixmap)
                                            cursor))
                                        props))
  (graphics/init)
  (ui/load! :skin-scale/x1)
  (bind-root #'app/screens (mapvals stage-screen (screens/init)))
  (app/change-screen screens/first-k))

(defn dispose []
  (assets/dispose)
  (run! utils/dispose (vals cursors/cursors))
  (graphics/dispose)
  (run! app/dispose (vals app/screens))
  (ui/dispose!))

(defn render []
  (clear-screen color/black)
  (app/render (app/current-screen)))

(defn resize [w h]
  (.update graphics/gui-viewport   w h true)
  (.update graphics/world-viewport w h))
