(ns clojure.ui.player-state
  (:require [clojure.entity :as entity]
            [clojure.ui :refer [ui-actor]]))

(defn create [_context _config]
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @(:clojure.context/player-eid %))
                                          %)}))
