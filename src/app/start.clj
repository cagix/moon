(ns app.start
  (:require [app.screens.editor :as property-editor]
            [app.screens.main :as main-menu]
            [app.screens.map-editor :as map-editor]
            [app.screens.world :as world-screen]
            [clojure.string :as str]
            [component.db :as db]
            [gdx.assets :as assets]
            [gdx.graphics :as g]
            [gdx.ui :as ui]
            [gdx.screen :as screen]
            [gdx.vis-ui :as vis-ui])
  (:import (com.badlogic.gdx ApplicationAdapter
                             Gdx)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application
                                             Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Color
                                      Texture)
           (com.badlogic.gdx.utils SharedLibraryLoader
                                   ScreenUtils)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration))
  #_(:gen-class))

(def cursors {:cursors/bag                   ["bag001"       [0   0]]
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
              :cursors/walking               ["walking"      [16 16]]})

(def graphics {:cursors cursors
               :default-font {:file "fonts/exocet/films.EXL_____.ttf"
                              :size 16
                              :quality-scaling 2}
               :views {:gui-view {:world-width 1440
                                  :world-height 900}
                       :world-view {:world-width 1440
                                    :world-height 900
                                    :tile-size 48}}})

(defn- moon []
  (ui/image->widget (g/image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (.internal Gdx/files folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- search-assets [folder]
  (for [[class exts] [[Sound #{"wav"}]
                      [Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (recursively-search folder exts))]
    [file class]))

(defn- application-listener []
  (proxy [ApplicationAdapter] []
    (create []
      (assets/load (search-assets "resources/"))
      (g/load! graphics)
      (vis-ui/load! :skin-scale/x1)
      (screen/set-screens! [(main-menu/create moon)
                            (map-editor/create)
                            (property-editor/screen moon)
                            (world-screen/create)])
      ((world-screen/start-game-fn :worlds/vampire)))

    (dispose []
      (assets/dispose)
      (g/dispose!)
      (vis-ui/dispose!)
      (screen/dispose-all!))

    (render []
      (ScreenUtils/clear Color/BLACK)
      (screen/render! (screen/current)))

    (resize [w h]
      (g/resize! [w h]))))

(defn- set-dock-icon [image-path]
  (let [toolkit (Toolkit/getDefaultToolkit)
        image (.getImage toolkit (clojure.java.io/resource image-path))
        taskbar (Taskbar/getTaskbar)]
    (.setIconImage taskbar image)))

(defn -main []
  (db/load! "properties.edn")
  (when SharedLibraryLoader/isMac
    (set-dock-icon "moon.png")
    (.set Configuration/GLFW_LIBRARY_NAME "glfw_async")
    (.set Configuration/GLFW_CHECK_THREAD0 false))
  (Lwjgl3Application. (application-listener)
                      (doto (Lwjgl3ApplicationConfiguration.)
                        (.setTitle "Eternal")
                        (.setForegroundFPS 60)
                        (.setWindowedMode 1440 900))))
