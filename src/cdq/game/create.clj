(ns cdq.game.create
  (:require [cdq.game.create.tx-handler :as create-tx-handler]
            [cdq.game.create.db :as create-db]
            [cdq.game.create.graphics :as create-graphics]
            [cdq.game.create.ui :as create-ui]
            [cdq.game.create.input-processor :as create-input-processor]
            [cdq.game.create.audio :as create-audio]
            [cdq.game.create.world :as create-world]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [qrecord.core :as q]))

(q/defrecord Context [])

(def graphics-params
  {:tile-size 48
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
                    :cursors/walking               ["walking"      [16 16]]}}})

(def audio-config {
                   :sound-names (->> "sounds.edn" io/resource slurp edn/read-string)
                   :path-format "sounds/%s.wav"
                   })

(defn do! [gdx]
  (-> {:ctx/gdx gdx}
      map->Context
      create-tx-handler/do!
      create-db/do!
      (create-graphics/do! graphics-params)
      (create-ui/do! '[[cdq.ctx.create.ui.dev-menu/create cdq.game.create.world/do!]
                       [cdq.ctx.create.ui.action-bar/create]
                       [cdq.ctx.create.ui.hp-mana-bar/create]
                       [cdq.ctx.create.ui.windows/create [[cdq.ctx.create.ui.windows.entity-info/create]
                                                          [cdq.ctx.create.ui.windows.inventory/create]]]
                       [cdq.ctx.create.ui.player-state-draw/create]
                       [cdq.ctx.create.ui.message/create]])
      create-input-processor/do!
      (create-audio/do! audio-config)
      (create-world/do! "world_fns/vampire.edn")))
