(ns cdq.entity.state.player-dead
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [gdl.utils :refer [defcomponent]]))

(defcomponent :player-dead
  (state/cursor [_] :cursors/black-x)

  (state/pause-game? [_] true)

  (state/enter! [_ _eid]
    [[:tx/sound "bfxr_playerdeath"]
     [:tx/show-modal {:title "YOU DIED - again!"
                      :text "Good luck next time!"
                      :button-text "OK"
                      :on-click (fn [])}]]))
