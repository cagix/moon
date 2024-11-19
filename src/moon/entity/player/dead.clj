(ns moon.entity.player.dead
  (:require [moon.app :refer [change-screen play-sound]]
            [moon.widgets.modal :as modal]))

(defn cursor [_]
  :cursors/black-x)

(defn pause-game? [_]
  true)

(defn enter [_]
  (play-sound "sounds/bfxr_playerdeath.wav")
  (modal/show {:title "YOU DIED"
               :text "\nGood luck next time"
               :button-text ":("
               :on-click #(change-screen :screens/main-menu)}))
