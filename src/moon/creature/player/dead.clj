(ns ^:no-doc moon.creature.player.dead
  (:require [moon.component :refer [defc]]
            [moon.screen :as screen]
            [moon.entity.state :as state]))

(defc :player-dead
  (state/player-enter [_]
    [[:tx/cursor :cursors/black-x]])

  (state/pause-game? [_]
    true)

  (state/enter [_]
    [[:tx/sound "sounds/bfxr_playerdeath.wav"]
     [:tx/player-modal {:title "YOU DIED"
                        :text "\nGood luck next time"
                        :button-text ":("
                        :on-click #(screen/change! :screens/main-menu)}]]))
