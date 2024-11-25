(ns app.start
  (:require app.methods
            [app.screens.editor :as editor]
            [app.screens.map-editor :as map-editor]
            [app.screens.minimap :as minimap]
            [app.screens.world :as world]
            [forge.app :refer [start-app change-screen set-cursor]]
            [forge.db :as db]
            [forge.screen :as screen]
            [gdl.input :refer [key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.utils :refer [dev-mode?]]
            [moon.widgets.background-image :as background-image])
  (:import (com.badlogic.gdx Gdx)))

(defn- main-menu []
  {:actors [(background-image/create)
            (ui/table
             {:rows
              (remove nil?
                      (concat
                       (for [world (db/all :properties/worlds)]
                         [(ui/text-button (str "Start " (:property/id world))
                                          #(world/start world))])
                       [(when dev-mode?
                          [(ui/text-button "Map editor"
                                           #(change-screen :screens/map-editor))])
                        (when dev-mode?
                          [(ui/text-button "Property editor"
                                           #(change-screen :screens/editor))])
                        [(ui/text-button "Exit"
                                         #(.exit Gdx/app))]]))
              :cell-defaults {:pad-bottom 25}
              :fill-parent? true})
            (ui/actor {:act (fn []
                              (when (key-just-pressed? :keys/escape)
                                (.exit Gdx/app)))})]
   :screen (reify screen/Screen
             (screen/enter [_]
               (set-cursor :cursors/default))
             (screen/exit [_])
             (screen/render [_])
             (screen/dispose [_]))})

(def ^:private config
  {:cursors {:cursors/bag                   ["bag001"       [0   0]]
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
   :tile-size 48
   :world-viewport-width 1440
   :world-viewport-height 900
   :gui-viewport-width 1440
   :gui-viewport-height 900
   :ui-skin-scale :skin-scale/x1
   :init-screens (fn []
                   {:screens/main-menu  (main-menu)
                    :screens/map-editor (map-editor/create)
                    :screens/editor     (editor/create)
                    :screens/minimap    (minimap/create)
                    :screens/world      (world/create)})
   :first-screen-k :screens/main-menu})

(defn -main []
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (start-app config))
