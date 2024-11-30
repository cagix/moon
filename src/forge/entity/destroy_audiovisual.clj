(ns ^:no-doc forge.entity.destroy-audiovisual
  (:require [forge.world :as world]))

(defn destroy [audiovisuals-id eid]
  (world/audiovisual (:position @eid) (build audiovisuals-id)))
