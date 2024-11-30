(ns ^:no-doc forge.entity.player.dead
  (:require [forge.ui.modal :as modal]))

(defn cursor [_]
  :cursors/black-x)

(defn pause-game? [_]
  true)

(defn enter [_]
  (play-sound "bfxr_playerdeath")
  (modal/show {:title "YOU DIED"
               :text "\nGood luck next time"
               :button-text ":("
               :on-click #(change-screen :screens/main-menu)}))
