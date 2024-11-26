(ns ^:no-doc app.start
  (:require [app.dock-icon :as dock-icon]
            [app.lifecycle :as lifecycle]
            [app.screens.editor :as editor]
            [app.screens.main :as main]
            [app.screens.map-editor :as map-editor]
            [app.screens.minimap :as minimap]
            [app.world :as world]
            app.systems
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils :refer [mac?]]
            [forge.db :as db]
            forge.entity.animation
            (mapgen generate uf-caves))
  (:import (com.badlogic.gdx ApplicationAdapter)
           (org.lwjgl.system Configuration)))

(defn -main []
  (db/init :schema "schema.edn"
           :properties "properties.edn")
  (dock-icon/set-image "moon.png")
  (when mac?
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (lwjgl3/application (proxy [ApplicationAdapter] []
                        (create  []
                          (lifecycle/create :screens/main-menu
                                            (fn []
                                              {:screens/main-menu  (main/create)
                                               :screens/map-editor (map-editor/create)
                                               :screens/editor     (editor/create)
                                               :screens/minimap    (minimap/create)
                                               :screens/world      (world/screen)})))
                        (dispose []
                          (lifecycle/dispose))

                        (render  []
                          (lifecycle/render))

                        (resize  [w h]
                          (lifecycle/resize w h)))
                      (lwjgl3/config {:title "Moon"
                                      :fps 60
                                      :width 1440
                                      :height 900})))
