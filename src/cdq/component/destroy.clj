(ns cdq.component.destroy
  (:require [gdl.context :as c]
            [cdq.context :as world]
            [clojure.component :as component]))

(defmethod component/destroy :entity/destroy-audiovisual
  [[_ audiovisuals-id] eid c]
  (world/audiovisual c
                     (:position @eid)
                     (c/build c audiovisuals-id)))
