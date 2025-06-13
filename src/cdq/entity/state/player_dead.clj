(ns cdq.entity.state.player-dead
  (:require [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.utils :refer [defmethods]]))

(defmethods :player-dead
  (state/enter! [_ _eid]
    [[:tx/sound "bfxr_playerdeath"]
     [:tx/show-modal {:title "YOU DIED - again!"
                      :text "Good luck next time!"
                      :button-text "OK"
                      :on-click (fn [])}]]))
