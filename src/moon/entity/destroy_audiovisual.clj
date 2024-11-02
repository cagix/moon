(ns moon.entity.destroy-audiovisual
  (:require [moon.entity :as entity]))

(defmethods :entity/destroy-audiovisual
  (entity/destroy [[_ audiovisuals-id] eid]
    [[:tx/audiovisual (:position @eid) audiovisuals-id]]))
