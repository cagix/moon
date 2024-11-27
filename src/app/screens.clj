(ns app.screens
  (:require [forge.screens.editor :as editor]
            [forge.screens.main :as main]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [app.world :as world]))

(defn init []
  {:screens/main-menu  (main/create)
   :screens/map-editor (map-editor/create)
   :screens/editor     (editor/create)
   :screens/minimap    (minimap/create)
   :screens/world      (world/screen)})

(def first-k :screens/main-menu)
