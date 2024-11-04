(ns moon.entity.player.dead
  (:require [gdl.screen :as screen]))

(defn player-enter [_]
  [[:tx/cursor :cursors/black-x]])

(defn pause-game? [_]
  true)

(defn enter [_]
  [[:tx/sound "sounds/bfxr_playerdeath.wav"]
   [:tx/player-modal {:title "YOU DIED"
                      :text "\nGood luck next time"
                      :button-text ":("
                      :on-click #(screen/change :screens/main-menu)}]])
