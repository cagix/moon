(ns moon.entity.player.dead
  (:require [gdl.assets :refer [play-sound]]
            [gdl.screen :as screen]
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
               :on-click #(screen/change :screens/main-menu)})
  nil)
