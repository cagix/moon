(ns moon.entity.destroy-audiovisual
  (:require [moon.audiovisual :as audiovisual]))

(defn destroy [audiovisuals-id eid]
  (audiovisual/create (:position @eid) audiovisuals-id))
