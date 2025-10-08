(ns cdq.application
  (:require [cdq.game.create :as create]
            [cdq.game.dispose :as dispose]
            [cdq.game.render :as render]
            [cdq.game.resize :as resize]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.badlogic.gdx.backends.lwjgl :as lwjgl])
  (:gen-class))

(def config
  {
   :audio {
           :sound-names (->> "sounds.edn" io/resource slurp edn/read-string)
           :path-format "sounds/%s.wav"
           }

   :graphics {:tile-size 48
              :ui-viewport {:width 1440
                            :height 900}
              :world-viewport {:width 1440
                               :height 900}
              :texture-folder {:folder "resources/"
                               :extensions #{"png" "bmp"}}
              :default-font {:path "exocet/films.EXL_____.ttf"
                             :params {:size 16
                                      :quality-scaling 2
                                      :enable-markup? true
                                      :use-integer-positions? false
                                      ; :texture-filter/linear because scaling to world-units
                                      :min-filter :linear
                                      :mag-filter :linear}}
              :cursors {:path-format "cursors/%s.png"
                        :data {:cursors/bag                   ["bag001"       [0   0]]
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
                               :cursors/walking               ["walking"      [16 16]]}}}
   }
  )

(def state (atom nil))

(defn -main []
  (lwjgl/application
   {
    :title "Cyber Dungeon Quest"

    :window {:width 1440
             :height 900}

    :fps 60

    :create! (fn [gdx]
               (reset! state (create/do! gdx config)))

    :dispose! (fn []
                (dispose/do! @state))

    :render! (fn []
               (swap! state render/do!))

    :resize! (fn [width height]
               (resize/do! @state width height))

    :colors {"PRETTY_NAME" [0.84 0.8 0.52 1]}
    }))
