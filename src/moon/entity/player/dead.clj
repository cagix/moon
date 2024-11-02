(ns moon.entity.player.dead
  (:require [moon.entity :as entity]
            [moon.screen :as screen]))

(defmethods :player-dead
  (entity/player-enter [_]
    [[:tx/cursor :cursors/black-x]])

  (entity/pause-game? [_]
    true)

  (entity/enter [_]
    [[:tx/sound "sounds/bfxr_playerdeath.wav"]
     [:widgets/player-modal {:title "YOU DIED"
                             :text "\nGood luck next time"
                             :button-text ":("
                             :on-click #(screen/change :screens/main-menu)}]]))
