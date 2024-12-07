(ns forge.entity.state.player-dead
  (:require [clojure.utils :refer [defmethods]])
  )

(defmethods :player-dead
  (state-cursor [_]
    :cursors/black-x)

  (pause-game? [_]
    true)

  (state-enter [_]
    (play-sound "bfxr_playerdeath")
    (show-modal {:title "YOU DIED"
                 :text "\nGood luck next time"
                 :button-text ":("
                 :on-click #(change-screen :screens/main-menu)})))
