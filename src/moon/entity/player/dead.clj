(ns ^:no-doc moon.entity.player.dead
  (:require [forge.app :as app]
            [forge.assets :refer [play-sound]]
            [forge.ui.modal :as modal]))

(defn cursor [_]
  :cursors/black-x)

(defn pause-game? [_]
  true)

(defn enter [_]
  (play-sound "bfxr_playerdeath")
  (modal/show {:title "YOU DIED"
               :text "\nGood luck next time"
               :button-text ":("
               :on-click #(app/change-screen :screens/main-menu)}))
