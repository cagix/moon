(ns forge.entity.state.player-dead
  (:require [anvil.app :refer [change-screen]]
            [anvil.audio :refer [play-sound]]
            [anvil.stage :refer [show-modal]]))

(defn cursor [_]
  :cursors/black-x)

(defn pause-game? [_]
  true)

(defn enter [_]
  (play-sound "bfxr_playerdeath")
  (show-modal {:title "YOU DIED"
               :text "\nGood luck next time"
               :button-text ":("
               :on-click #(change-screen :screens/main-menu)}))
