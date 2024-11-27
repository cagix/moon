(ns app.start
  (:require [app.lifecycle :as lifecycle]
            [app.systems]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils :refer [mac?]]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [forge.db :as db]
            [forge.screens.editor :as editor]
            [forge.screens.main :as main]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world])
  (:import (com.badlogic.gdx.graphics Pixmap)))

(def ^:private cursors*
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

(defn- cursors []
  (mapvals (fn [[file hotspot]]
             (let [pixmap (Pixmap. (gdx/internal-file (str "cursors/" file ".png")))
                   cursor (gdx/new-cursor pixmap hotspot)]
               (.dispose pixmap)
               cursor))
           cursors*))

(defn- screens []
  {:screens/main-menu  (main/create)
   :screens/map-editor (map-editor/create)
   :screens/editor     (editor/create)
   :screens/minimap    (minimap/create)
   :screens/world      (world/screen)})

(defn -main []
  (awt/set-dock-icon "moon.png")
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (when mac?
    (lwjgl3/configure-glfw-for-mac))
  (lwjgl3/application (proxy [com.badlogic.gdx.ApplicationAdapter] []
                        (create  []    (lifecycle/create
                                        cursors
                                        :skin-scale/x1
                                        screens
                                        :screens/main-menu
                                        ))
                        (dispose []    (lifecycle/dispose))
                        (render  []    (lifecycle/render))
                        (resize  [w h] (lifecycle/resize w h)))
                      (lwjgl3/config {:title "Moon"
                                      :fps 60
                                      :width 1440
                                      :height 900})))
