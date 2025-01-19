(ns cdq.render.update-paused
  (:require cdq.entity.state
            [clojure.gdx.input :as input]))

(defn render [{:keys [cdq.context/player-eid
                      error ; FIXME ! not `::` keys so broken !
                      ] :as c}]
  (let [pausing? true]
    (assoc c :cdq.context/paused? (or error
                                      (and pausing?
                                           (cdq.entity.state/pause-game? (cdq.entity/state-obj @player-eid))
                                           (not (or (input/key-just-pressed? :p)
                                                    (input/key-pressed?      :space))))))))
