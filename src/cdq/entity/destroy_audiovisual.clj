(ns cdq.entity.destroy-audiovisual
  (:require [gdl.context :as c]
            [cdq.context :as world]))

(defn destroy [[_ audiovisuals-id] eid c]
  (world/audiovisual c
                     (:position @eid)
                     (c/build c audiovisuals-id)))
