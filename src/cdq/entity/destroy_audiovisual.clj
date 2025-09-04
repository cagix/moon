(ns cdq.entity.destroy-audiovisual
  (:require [cdq.world.entity :as entity]))

(defn destroy! [audiovisuals-id eid _ctx]
  [[:tx/audiovisual (entity/position @eid) audiovisuals-id]])
