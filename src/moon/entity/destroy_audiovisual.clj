(ns moon.entity.destroy-audiovisual)

(defn destroy [audiovisuals-id eid]
  [[:tx/audiovisual (:position @eid) audiovisuals-id]])
