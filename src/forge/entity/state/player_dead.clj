(ns forge.entity.state.player-dead
  (:require [clojure.utils :refer [defmethods]]
            [forge.app.asset-manager :refer [play-sound]]
            [forge.app.screens :refer [change-screen]]
            [forge.entity.state :refer [cursor pause-game? enter]]
            [forge.ui :refer [show-modal]]))

(defmethods :player-dead
  (cursor [_]
    :cursors/black-x)

  (pause-game? [_]
    true)

  (enter [_]
    (play-sound "bfxr_playerdeath")
    (show-modal {:title "YOU DIED"
                 :text "\nGood luck next time"
                 :button-text ":("
                 :on-click #(change-screen :screens/main-menu)})))
