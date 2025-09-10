(ns cdq.application.gdx-create
  (:require [cdq.files]
            [cdq.gdx.graphics]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.files :as files]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.vis-ui :as vis-ui]))

(def sounds "sounds.edn")
(def sound-path-format "sounds/%s.wav")

(defn after-gdx-create!
  [ctx]
  (vis-ui/load! {:skin-scale :x1})
  (let [graphics (cdq.gdx.graphics/create
                  (gdx/graphics)
                  {
                   :ui-viewport {:width 1440
                                 :height 900}
                   :default-font {:file "exocet/films.EXL_____.ttf"
                                  :params {:size 16
                                           :quality-scaling 2
                                           :enable-markup? true
                                           :use-integer-positions? false} ; false, otherwise scaling to world-units not visible
                                  }
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
                                    :cursors/walking               ["walking"      [16 16]]}
                             }
                   :world-unit-scale (float (/ 48))
                   :world-viewport {:width 1440 :height 900}
                   :textures-to-load (cdq.files/search (gdx/files)
                                                       {:folder "resources/"
                                                        :extensions #{"png" "bmp"}})
                   })
        input (gdx/input)
        stage (scene2d/stage (:ctx/ui-viewport graphics)
                             (:ctx/batch graphics))]
    (input/set-processor! input stage)
    (merge ctx
           graphics
           {:ctx/input input
            :ctx/stage stage
            :ctx/audio (into {}
                             (for [sound-name (->> sounds io/resource slurp edn/read-string)
                                   :let [path (format sound-path-format sound-name)]]
                               [sound-name
                                (audio/sound (gdx/audio) (files/internal (gdx/files) path))]))})))
