(ns cdq.ui.player-state
  (:require [cdq.entity :as entity]
            [clojure.ui :refer [ui-actor]]))

(defn create [_context _config]
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @(:cdq.context/player-eid %))
                                          %)}))
