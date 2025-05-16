(ns cdq.entity.state.player-dead
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [defcomponent]]))

(defcomponent :player-dead
  (state/cursor [_] :cursors/black-x)

  (state/pause-game? [_] true)

  (state/enter! [_]
    [[:tx/sound "bfxr_playerdeath"]
     [:tx/show-modal {:title "YOU DIED - again!"
                      :text "Good luck next time!"
                      :button-text "OK"
                      :on-click (fn [])}]]))
