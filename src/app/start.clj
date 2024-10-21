(ns app.start
  (:require [app.screens.editor :as property-editor]
            [app.screens.main :as main-menu]
            [app.screens.map-editor :as map-editor]
            [app.screens.world :as world-screen]
            [clojure.string :as str]
            [component.db :as db]
            [gdx.app :as app]
            [gdx.assets :as assets]
            [gdx.files :as files]
            [gdx.graphics :as g]
            [gdx.ui :as ui]
            [gdx.screen :as screen]
            [gdx.vis-ui :as vis-ui])
  (:import (com.badlogic.gdx ApplicationAdapter)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Color Texture)
           (com.badlogic.gdx.utils ScreenUtils)))

(def lwjgl3-config {:title "Core"
                    :width 1440
                    :height 900
                    :full-screen? false
                    :fps 60})

(def cursors {:cursors/bag ["bag001" [0 0]]
              :cursors/black-x ["black_x" [0 0]]
              :cursors/default ["default" [0 0]]
              :cursors/denied ["denied" [16 16]]
              :cursors/hand-before-grab ["hand004" [4 16]]
              :cursors/hand-before-grab-gray ["hand004_gray" [4 16]]
              :cursors/hand-grab ["hand003" [4 16]]
              :cursors/move-window ["move002" [16 16]]
              :cursors/no-skill-selected ["denied003" [0 0]]
              :cursors/over-button ["hand002" [0 0]]
              :cursors/sandclock ["sandclock" [16 16]]
              :cursors/skill-not-usable ["x007" [0 0]]
              :cursors/use-skill ["pointer004" [0 0]]
              :cursors/walking ["walking" [16 16]]})

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

(defn- search-assets [folder]
  (for [[class exts] [[Sound #{"wav"}]
                      [Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (files/recursively-search folder exts))]
    [file class]))

(defn -main []
  (db/load! "properties.edn")
  (app/start! (proxy [ApplicationAdapter] []
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
                  (g/resize! [w h])))
              lwjgl3-config))
