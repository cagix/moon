(ns gdl.editor
  (:require [gdl.app :as app]))

(def ^:private config
  {:config {:title "Editor"
            :fps 60
            :width 1440
            :height 900
            :taskbar-icon "icon.png"}
   :gdl-config {:assets "resources/"
                :db {:schema "schema.edn"
                     :properties "properties.edn"}
                :default-font {:file "fonts/exocet/films.EXL_____.ttf"
                               :size 16
                               :quality-scaling 2}
                :cursors {:cursors/bag                   ["bag001"       [0   0]]
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
                :ui-viewport {:width 1440 :height 900}
                :tile-size -1
                :world-viewport {:width 1440 :height 900}
                :vis-ui {:skin-scale :skin-scale/x1}}
   :context [[:gdl.editor/actors]]
   :transactions '[gdl.context/update-stage
                   gdl.graphics/draw-stage]})

(defn -main []
  (app/start config))
