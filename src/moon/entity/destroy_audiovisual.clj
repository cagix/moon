(ns moon.entity.destroy-audiovisual)

(defn destroy [[_ audiovisuals-id] eid]
  [[:tx/audiovisual (:position @eid) audiovisuals-id]])
