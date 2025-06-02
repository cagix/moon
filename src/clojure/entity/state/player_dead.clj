(ns clojure.entity.state.player-dead
  (:require [clojure.entity :as entity]
            [clojure.state :as state]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :player-dead
  (state/cursor [_] :cursors/black-x)

  (state/pause-game? [_] true)

  (state/enter! [_ _eid]
    [[:tx/sound "bfxr_playerdeath"]
     [:tx/show-modal {:title "YOU DIED - again!"
                      :text "Good luck next time!"
                      :button-text "OK"
                      :on-click (fn [])}]]))
