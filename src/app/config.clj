(ns app.config)

(def properties "properties.edn")

(def lwjgl3 {:title "Eternal"
             :fps 60
             :width 1440
             :height 900
             :dock-icon "moon.png"})

(def resources "resources/")

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

(def skin-scale :skin-scale/x1)

(def screen-background "images/moon_background.png")
