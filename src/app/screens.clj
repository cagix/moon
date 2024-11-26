(ns app.screens
  (:require [app.screens.editor :as editor]
            [app.screens.main :as main]
            [app.screens.map-editor :as map-editor]
            [app.screens.minimap :as minimap]
            [app.world :as world]))

(defn init []
  {:screens/main-menu  (main/create)
   :screens/map-editor (map-editor/create)
   :screens/editor     (editor/create)
   :screens/minimap    (minimap/create)
   :screens/world      (world/screen)})

(def first-k :screens/main-menu)
