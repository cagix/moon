(ns cdq.entity.destroy-audiovisual
  (:require [cdq.entity :as entity]))

(defn destroy! [audiovisuals-id eid _world]
  [[:tx/audiovisual (entity/position @eid) audiovisuals-id]])
