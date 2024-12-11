(ns anvil.app
  (:require [anvil.assets :as assets]
            [anvil.db :as db]
            [anvil.graphics :as g]
            [anvil.screen :as screen]
            [anvil.screens.editor :as editor]
            [anvil.screens.minimap :as minimap]
            [anvil.screens.world :as world]
            [anvil.sprite :as sprite]
            [anvil.ui :as ui]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.world.create :refer [create-world]])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.utils ScreenUtils SharedLibraryLoader)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- clear-screen []
  (ScreenUtils/clear Color/BLACK))

(defn- set-dock-icon [icon]
  (.setIconImage (Taskbar/getTaskbar)
                 (.getImage (Toolkit/getDefaultToolkit)
                            (io/resource icon))))

(defn- start-app [{:keys [title fps width height]} listener]
  (when SharedLibraryLoader/isMac
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. listener
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle title)
                        (.setForegroundFPS fps)
                        (.setWindowedMode width height))))

(defn- start [{:keys [db dock-icon lwjgl3-config assets graphics ui world-id]}]
  (db/setup db)
  (set-dock-icon dock-icon)
  (start-app lwjgl3-config
             (proxy [ApplicationAdapter] []
               (create []
                 (assets/setup assets)
                 (g/setup graphics)
                 (ui/setup ui)
                 (screen/setup {:screens/editor (editor/create)
                                :screens/minimap (minimap/screen)
                                :screens/world (world/screen)}
                               :screens/world)
                 (create-world (db/build world-id)))

               (dispose []
                 (assets/cleanup)
                 (g/cleanup)
                 (ui/cleanup)
                 (screen/cleanup))

               (render []
                 (clear-screen)
                 (screen/render-current))

               (resize [w h]
                 (g/resize w h)))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
