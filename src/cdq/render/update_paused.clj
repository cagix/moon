(ns cdq.render.update-paused
  (:require [clojure.gdx.input :as input]))

(def state-k->pause-game?
  {:stunned false
   :player-moving false
   :player-item-on-cursor true
   :player-idle true
   :player-dead true
   :active-skill false})

(defn render [{:keys [cdq.context/player-eid
                      error ; FIXME ! not `::` keys so broken !
                      ] :as c}]
  (let [pausing? true]
    (assoc c :cdq.context/paused? (or error
                                      (and pausing?
                                           (state-k->pause-game? (cdq.entity/state-k @player-eid))
                                           (not (or (input/key-just-pressed? :p)
                                                    (input/key-pressed?      :space))))))))
