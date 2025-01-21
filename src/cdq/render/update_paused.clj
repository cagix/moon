(ns cdq.render.update-paused
  (:require [clojure.gdx.input :as input]))

(defn render [{:keys [cdq.context/player-eid
                      context/entity-components
                      error ; FIXME ! not `::` keys so broken !
                      ] :as c}]
  (let [pausing? true]
    (assoc c :cdq.context/paused? (or error
                                      (and pausing?
                                           (get-in entity-components [(cdq.entity/state-k @player-eid) :pause-game?])
                                           (not (or (input/key-just-pressed? :p)
                                                    (input/key-pressed?      :space))))))))
