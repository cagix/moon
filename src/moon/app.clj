(ns moon.app
  (:require [clojure.string :as str]
            [gdl.app :as app]
            [gdl.assets :refer [manager]]
            [gdl.ui :as ui]
            [gdl.utils :refer [dispose recursively-search]]
            [moon.db :as db]
            [moon.assets :as assets]
            [moon.graphics :as g]
            [moon.screen :as screen]
            (moon.screens [editor :as property-editor]
                          [main :as main-menu]
                          [map-editor :as map-editor]
                          [world :as world-screen])))

(defn- search-assets [folder]
  (for [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                      [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (recursively-search folder exts))]
    [file class]))

(def ^:private cursors
  {:cursors/bag                   ["bag001"       [0   0]]
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

(def ^:private graphics
  {:cursors cursors
   :default-font {:file "fonts/exocet/films.EXL_____.ttf"
                  :size 16
                  :quality-scaling 2}
   :views {:gui-view {:world-width 1440
                      :world-height 900}
           :world-view {:world-width 1440
                        :world-height 900
                        :tile-size 48}}})

(defn- background-image []
  (ui/image->widget (g/image "images/moon_background.png")
                    {:fill-parent? true
                     :scaling :fill
                     :align :center}))

(defn- app-listener []
  (reify app/Listener
    (create [_]
      (.bindRoot #'assets/manager (manager (search-assets "resources/")))
      (g/load! graphics)
      (ui/load! :skin-scale/x1)
      (screen/set-screens! [(main-menu/create background-image)
                            (map-editor/create)
                            (property-editor/screen background-image)
                            (world-screen/create)])
      ((world-screen/start-game-fn :worlds/vampire)))

    (dispose [_]
      (dispose assets/manager)
      (g/dispose!)
      (ui/dispose!)
      (screen/dispose-all!))

    (render [_]
      (screen/render! (screen/current)))

    (resize [_ dimensions]
      (g/resize! dimensions))))

(defn -main []
  (db/load! "properties.edn")
  (when app/mac?
    (app/set-dock-icon "moon.png"))
  (app/start {:title "Moon"
              :fps 60
              :width 1440
              :height 900}
             (app-listener)))
