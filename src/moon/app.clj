(ns moon.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.assets :as assets]
            [gdl.app :as app]
            [gdl.graphics :refer [clear-screen]]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.shape-drawer :as shape-drawer]
            [gdl.ui :as ui]
            [moon.db :as db]
            [moon.graphics.cursors :as cursors]
            [moon.graphics.gui-view :as gui-view]
            [moon.graphics.world-view :as world-view]
            [moon.graphics.tiled :as graphics.tiled]
            [moon.graphics.text :as font]
            [moon.screen :as screen]
            [moon.screens.main :as main-menu]
            [moon.screens.editor :as editor]
            [moon.screens.map-editor :as map-editor]
            [moon.screens.minimap :as minimap]
            [moon.screens.world :as world]
            moon.components))

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
                 (cursors/init)
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
