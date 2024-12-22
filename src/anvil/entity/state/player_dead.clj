(ns ^:no-doc anvil.entity.state.player-dead
  (:require [anvil.component :as component]
            [gdl.assets :refer [play-sound]]
            [gdl.stage :refer [show-modal]]))

(defmethods :player-dead
  (component/enter [_]
    (play-sound "bfxr_playerdeath")
    (show-modal {:title "YOU DIED"
                 :text "\nGood luck next time"
                 :button-text ":("
                 :on-click (fn [])})))
