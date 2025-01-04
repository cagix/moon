(ns cdq.context.spawn-enemies
  (:require [clojure.gdx.tiled :as tiled]
            [gdl.utils :refer [tile->middle]]
            [cdq.context :refer [spawn-creature]]))

(defn- spawn-enemies [c tiled-map]
  (doseq [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                  {:position position
                   :creature-id (keyword creature-id)
                   :components {:entity/fsm {:fsm :fsms/npc
                                             :initial-state :npc-sleeping}
                                :entity/faction :evil}})]
    (spawn-creature c (update props :position tile->middle))))

(defn create [_ c]
  (spawn-enemies c (:cdq.context/tiled-map c)))
