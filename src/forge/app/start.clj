(ns forge.app.start
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils :refer [dispose clear-screen]]
            [clojure.java.io :as io]
            [forge.application :as application]
            [forge.app :as app]
            [forge.app.systems]
            [forge.screens.editor :as editor]
            [forge.screens.main :as main]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world]
            [forge.assets :as assets]
            [forge.assets.manager :as asset-manager]
            [forge.db :as db]
            [forge.graphics :as graphics]
            [forge.graphics.cursors :as cursors]
            [forge.stage :as stage]
            [forge.ui :as ui]
            [forge.utils.files :as files])
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
               (dispose pixmap)
               cursor))
           cursors*))

(defn- screens []
  {:screens/main-menu  (main/create)
   :screens/map-editor (map-editor/create)
   :screens/editor     (editor/create)
   :screens/minimap    (minimap/create)
   :screens/world      (world/screen)})

(defn -main []
  (application/start {:dock-icon "moon.png"
                      :title "Moon"
                      :fps 60
                      :width 1440
                      :height 900}
                     (reify application/Listener
                       (create [_]
                         (db/init :schema "schema.edn"
                                  :properties "properties.edn")
                         (bind-root #'assets/manager (asset-manager/init
                                                      (files/search "resources/"
                                                                    [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                                                                     [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]])))
                         (bind-root #'cursors/cursors (cursors))
                         (graphics/init)
                         (ui/load! :skin-scale/x1)
                         (bind-root #'app/screens (mapvals stage/create (screens)))
                         (app/change-screen :screens/main-menu))

                       (dispose [_]
                         (dispose assets/manager)
                         (run! dispose (vals cursors/cursors))
                         (graphics/dispose)
                         (run! app/dispose (vals app/screens))
                         (ui/dispose!))

                       (render [_]
                         (clear-screen color/black)
                         (app/render (app/current-screen)))

                       (resize [_ w h]
                         (.update graphics/gui-viewport   w h true)
                         (.update graphics/world-viewport w h)))))
