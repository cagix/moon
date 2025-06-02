(ns clojure.entity.destroy-audiovisual
  (:require [clojure.entity :as entity]
            [clojure.utils :refer [defcomponent]]))

(defcomponent :entity/destroy-audiovisual
  (entity/destroy! [[_ audiovisuals-id] eid _ctx]
    [[:tx/audiovisual (entity/position @eid) audiovisuals-id]]))
