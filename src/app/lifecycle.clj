(ns app.lifecycle
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils :as utils :refer [clear-screen]]
            [forge.app :as app]
            [forge.assets :as assets]
            [forge.graphics :as graphics]
            [forge.graphics.cursors :as cursors]
            [forge.screens.editor :as editor]
            [forge.screens.main :as main]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world]
            [forge.stage :as stage]
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
  (bind-root #'app/screens (mapvals stage/create {:screens/main-menu  (main/create)
                                                  :screens/map-editor (map-editor/create)
                                                  :screens/editor     (editor/create)
                                                  :screens/minimap    (minimap/create)
                                                  :screens/world      (world/screen)}))
  (app/change-screen :screens/main-menu))

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
