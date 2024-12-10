(ns forge.app
  (:require [anvil.assets :as assets]
            [anvil.db :as db]
            [anvil.graphics :as g]
            [anvil.screen :as screen]
            [anvil.sprite :as sprite]
            [anvil.ui :as ui]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.main-menu :as main-menu]
            [forge.world :as world]
            [forge.editor :as editor]
            [forge.minimap :as minimap])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader ScreenUtils)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- background-image []
  (ui/image->widget (sprite/create "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defn- start [{:keys [db dock-icon asset-folder graphics ui-skin-scale title fps width height]}]
  (db/setup db)
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource dock-icon)))
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (proxy [ApplicationAdapter] []
                        (create  []
                          (assets/setup asset-folder)
                          (g/setup graphics)
                          (ui/setup ui-skin-scale)
                          (screen/setup {:screens/main-menu (main-menu/create background-image)
                                         ;:screens/map-editor
                                         :screens/editor (editor/create background-image)
                                         :screens/minimap (minimap/screen)
                                         :screens/world (world/screen)}
                                        :screens/main-menu))

                        (dispose []
                          (assets/cleanup)
                          (g/cleanup)
                          (ui/cleanup)
                          (screen/cleanup))

                        (render []
                          (ScreenUtils/clear g/black)
                          (screen/render-current))

                        (resize [w h]
                          (g/resize w h)))
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setForegroundFPS fps)
                        (.setWindowedMode width height))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
