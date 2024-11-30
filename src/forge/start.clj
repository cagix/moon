(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.app :as app]
            [forge.app.systems]
            [forge.assets :as assets]
            [forge.db :as db]
            [forge.graphics :as graphics]
            [forge.screens.editor :as editor]
            [forge.screens.main :as main]
            [forge.screens.map-editor :as map-editor]
            [forge.screens.minimap :as minimap]
            [forge.screens.world :as world]
            [forge.stage :as stage]
            [forge.ui :as ui])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- set-dock-icon [image-resource]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource image-resource))))

(defn- lwjgl3-config [{:keys [title fps width height]}]
  (doto (Lwjgl3ApplicationConfiguration.)
    (.setTitle title)
    (.setForegroundFPS fps)
    (.setWindowedMode width height)))

(def ^:private config "app.edn")

(defn -main []
  (let [{:keys [dock-icon
                lwjgl3
                db
                assets
                graphics
                ui]} (-> config io/resource slurp edn/read-string)]
    (set-dock-icon dock-icon)
    (when SharedLibraryLoader/isMac
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
      (.set Configuration/GLFW_CHECK_THREAD0 false))
    (Lwjgl3Application.
     (proxy [ApplicationAdapter] []
       (create []
         (db/init db)
         (assets/init assets)
         (graphics/init graphics)
         (ui/init ui)
         (bind-root #'app/screens
                    (mapvals stage/create
                             {:screens/main-menu  (main/create)
                              :screens/map-editor (map-editor/create)
                              :screens/editor     (editor/create)
                              :screens/minimap    (minimap/create)
                              :screens/world      (world/screen)}))
         (app/change-screen :screens/main-menu))

       (dispose []
         (assets/dispose)
         (graphics/dispose)
         (run! app/dispose (vals app/screens))
         (ui/dispose))

       (render []
         (graphics/clear-screen)
         (app/render-current-screen))

       (resize [w h]
         (graphics/resize w h)))
     (lwjgl3-config lwjgl3))))
