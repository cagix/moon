(ns app.lifecycle
  (:require [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils :as utils :refer [clear-screen]]
            [forge.app :as app]
            [forge.assets :as assets]
            [forge.graphics :as graphics]
            [forge.graphics.cursors :as cursors]
            [forge.stage :as stage]
            [forge.ui :as ui]))

(defn create [asset-folder
              cursors
              ui-skin-scale
              screens
              first-screen-k]
  (assets/init asset-folder)
  (bind-root #'cursors/cursors (cursors))
  (graphics/init)
  (ui/load! ui-skin-scale)
  (bind-root #'app/screens (mapvals stage/create (screens)))
  (app/change-screen first-screen-k))

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
