(ns moon.app
  (:require [gdl.assets :as assets]
            [gdl.app :as app]
            [gdl.graphics :refer [clear-screen]]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.cursors :as cursors]
            [gdl.graphics.tiled :as graphics.tiled]
            [gdl.graphics.text :as font]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.graphics.gui-view :as gui-view]
            [gdl.graphics.world-view :as world-view]
            [gdl.screen :as screen]
            [gdl.ui :as ui]
            [moon.db :as db]
            [moon.screens.main :as main-menu]
            [moon.screens.editor :as editor]
            [moon.screens.map-editor :as map-editor]
            [moon.screens.minimap :as minimap]
            [moon.screens.world :as world]
            moon.install))

(defn -main []
  (db/init)
  (app/start {:title "Moon"
              :fps 60
              :width 1440
              :height 900
              :dock-icon "moon.png"}
             (reify app/Listener
               (create [_]
                 (assets/init "resources/")
                 (batch/init)
                 (shape-drawer/init)
                 (cursors/init {:cursors/bag                   ["bag001"       [0   0]]
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
                 (gui-view/init {:world-width 1440
                                 :world-height 900})
                 (world-view/init {:world-width 1440
                                   :world-height 900
                                   :tile-size 48})
                 (ui/load! :skin-scale/x1)
                 (graphics.tiled/init)
                 (font/init {:file "fonts/exocet/films.EXL_____.ttf"
                             :size 16
                             :quality-scaling 2})
                 (screen/set-screens
                  {:screens/main-menu (main-menu/create)
                   :screens/map-editor (map-editor/create)
                   :screens/editor (editor/create)
                   :screens/minimap (minimap/create)
                   :screens/world (world/create)}))

               (dispose [_]
                 (assets/dispose)
                 (batch/dispose)
                 (shape-drawer/dispose)
                 (cursors/dispose)
                 (ui/dispose!)
                 (font/dispose)
                 (screen/dispose-all))

               (render [_]
                 (clear-screen :black)
                 (screen/render (screen/current)))

               (resize [_ dimensions]
                 (gui-view/resize   dimensions)
                 (world-view/resize dimensions)))))
