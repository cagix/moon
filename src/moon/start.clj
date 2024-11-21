(ns moon.start
  (:require [moon.app :refer [start-app]]
            [moon.db :as db]
            moon.methods
            [moon.screens.editor :as editor]
            [moon.screens.main :as main-menu]
            [moon.screens.map-editor :as map-editor]
            [moon.screens.minimap :as minimap]
            [moon.screens.world :as world]))

(def ^:private config
  {:app-config {:title "Moon"
                :fps 60
                :width 1440
                :height 900
                :dock-icon "moon.png"}
   :asset-folder "resources/"
   :cursors {:cursors/bag                   ["bag001"       [0   0]]
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
             :cursors/walking               ["walking"      [16 16]]}
   :default-font {:file "fonts/exocet/films.EXL_____.ttf"
                  :size 16
                  :quality-scaling 2}
   :tile-size 48
   :world-viewport-width 1440
   :world-viewport-height 900
   :gui-viewport-width 1440
   :gui-viewport-height 900
   :ui-skin-scale :skin-scale/x1
   :init-screens (fn []
                   {:screens/main-menu  (main-menu/create)
                    :screens/map-editor (map-editor/create)
                    :screens/editor     (editor/create)
                    :screens/minimap    (minimap/create)
                    :screens/world      (world/create)})
   :first-screen-k :screens/main-menu})

(defn -main []
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (start-app config))
