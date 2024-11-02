(ns moon.app
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.graphics :refer [clear-screen]]
            [gdl.ui :as ui]
            [moon.assets :as assets]
            [moon.db :as db]
            [moon.graphics.batch :as batch]
            [moon.graphics.cursors :as cursors]
            [moon.graphics.gui-view :as gui-view]
            [moon.graphics.world-view :as world-view]
            [moon.graphics.shape-drawer :as shape-drawer]
            [moon.graphics.tiled :as graphics.tiled]
            [moon.graphics.text :as font]
            [moon.screen :as screen]
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
                 (assets/init)
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
                 (screen/set-screens [:screens/main-menu
                                      :screens/map-editor
                                      :screens/editor
                                      :screens/minimap
                                      :screens/world]))

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
