(ns ^:no-doc anvil.entity.destroy-audiovisual
  (:require [anvil.entity :as entity]
            [gdl.context :as c]
            [cdq.context :as world]
            [clojure.utils :refer [defmethods]]))

(defmethods :entity/destroy-audiovisual
  (entity/destroy [[_ audiovisuals-id] eid c]
    (world/audiovisual c
                       (:position @eid)
                       (c/build c audiovisuals-id))))
