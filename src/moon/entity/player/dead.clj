(ns ^:no-doc moon.entity.player.dead
  (:require [forge.assets :refer [play-sound]]
            [forge.app :refer [change-screen show-modal]]))

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
