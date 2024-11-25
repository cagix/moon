(ns ^:no-doc moon.entity.player.dead
  (:require [forge.app :refer [change-screen play-sound show-modal]]))

(defn cursor [_]
  :cursors/black-x)

(defn pause-game? [_]
  true)

(defn enter [_]
  (play-sound "sounds/bfxr_playerdeath.wav")
  (show-modal {:title "YOU DIED"
               :text "\nGood luck next time"
               :button-text ":("
               :on-click #(change-screen :screens/main-menu)}))
